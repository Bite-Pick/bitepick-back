package com.magambell.server.servicearea.app.port.out.response;

import com.magambell.server.servicearea.domain.entity.ServiceArea;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "가까운 서비스 지역 추천 결과")
public record ServiceAreaRecommendationResponse(
        @Schema(description = "거리 오름차순 추천 지역 목록. 최대 3개이며 활성 지역이 없으면 빈 목록")
        List<Area> serviceAreas
) {
    @Schema(description = "추천 서비스 지역")
    public record Area(
            @Schema(description = "서비스 지역 ID", example = "1")
            Long id,
            @Schema(description = "서비스 지역 이름", example = "용인 죽전")
            String name,
            @Schema(description = "대표 위도", example = "37.331005")
            Double latitude,
            @Schema(description = "대표 경도", example = "127.113871")
            Double longitude
    ) {
        public static Area from(ServiceArea serviceArea) {
            return new Area(serviceArea.getId(), serviceArea.getName(),
                    serviceArea.getLatitude(), serviceArea.getLongitude());
        }
    }
}
