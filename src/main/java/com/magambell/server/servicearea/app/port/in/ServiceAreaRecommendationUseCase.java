package com.magambell.server.servicearea.app.port.in;

import com.magambell.server.servicearea.app.port.out.response.ServiceAreaRecommendationResponse;

public interface ServiceAreaRecommendationUseCase {

    ServiceAreaRecommendationResponse recommendServiceAreas(double latitude, double longitude);
}
