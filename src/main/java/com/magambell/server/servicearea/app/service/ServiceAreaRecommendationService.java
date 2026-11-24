package com.magambell.server.servicearea.app.service;

import com.magambell.server.servicearea.app.port.in.ServiceAreaRecommendationUseCase;
import com.magambell.server.servicearea.app.port.out.ServiceAreaQueryPort;
import com.magambell.server.servicearea.app.port.out.response.ServiceAreaRecommendationResponse;
import com.magambell.server.servicearea.domain.entity.ServiceArea;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class ServiceAreaRecommendationService implements ServiceAreaRecommendationUseCase {

    private static final int RECOMMENDATION_LIMIT = 3;
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private final ServiceAreaQueryPort serviceAreaQueryPort;

    @Override
    public ServiceAreaRecommendationResponse recommendServiceAreas(double latitude, double longitude) {
        var serviceAreas = serviceAreaQueryPort.findEnabledServiceAreas().stream()
                .sorted(Comparator
                        .comparingDouble((ServiceArea area) -> distanceMeters(latitude, longitude, area))
                        .thenComparing(ServiceArea::getId))
                .limit(RECOMMENDATION_LIMIT)
                .map(ServiceAreaRecommendationResponse.Area::from)
                .toList();

        return new ServiceAreaRecommendationResponse(serviceAreas);
    }

    private double distanceMeters(double latitude, double longitude, ServiceArea area) {
        double latitudeDifference = Math.toRadians(area.getLatitude() - latitude);
        double longitudeDifference = Math.toRadians(area.getLongitude() - longitude);
        double latitudeSine = Math.sin(latitudeDifference / 2);
        double longitudeSine = Math.sin(longitudeDifference / 2);
        double haversine = latitudeSine * latitudeSine
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(area.getLatitude()))
                * longitudeSine * longitudeSine;

        // 대척점 부근의 부동소수점 오차로 제곱근에 음수가 전달되지 않도록 한다.
        haversine = Math.max(0, Math.min(1, haversine));
        return 2 * EARTH_RADIUS_METERS * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    }
}
