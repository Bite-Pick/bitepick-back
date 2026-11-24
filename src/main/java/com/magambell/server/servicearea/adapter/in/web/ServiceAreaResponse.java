package com.magambell.server.servicearea.adapter.in.web;

import com.magambell.server.servicearea.domain.entity.ServiceArea;
import io.swagger.v3.oas.annotations.media.Schema;

public record ServiceAreaResponse(
        @Schema(description = "지역 ID", example = "1")
        Long id,
        
        @Schema(description = "지역 라벨", example = "경기도 용인시 기흥구 죽전동")
        String label,
        
        @Schema(description = "지역 이름", example = "경기도 용인시 기흥구 죽전동")
        String name,
        
        @Schema(description = "위도", example = "37.331005")
        Double latitude,
        
        @Schema(description = "경도", example = "127.113871")
        Double longitude
) {
    public static ServiceAreaResponse from(ServiceArea serviceArea) {
        return new ServiceAreaResponse(
                serviceArea.getId(),
                serviceArea.getLabel(),
                serviceArea.getName(),
                serviceArea.getLatitude(),
                serviceArea.getLongitude()
        );
    }
}
