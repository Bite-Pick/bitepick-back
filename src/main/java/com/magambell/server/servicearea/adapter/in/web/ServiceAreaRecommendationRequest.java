package com.magambell.server.servicearea.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record ServiceAreaRecommendationRequest(
        @Schema(description = "마지막 지도 검색 범위 중심 위도", example = "37.28",
                minimum = "-90", maximum = "90", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "위도를 입력해 주세요.")
        @DecimalMin(value = "-90", message = "위도는 -90 이상 90 이하여야 합니다.")
        @DecimalMax(value = "90", message = "위도는 -90 이상 90 이하여야 합니다.")
        Double latitude,

        @Schema(description = "마지막 지도 검색 범위 중심 경도", example = "127.04",
                minimum = "-180", maximum = "180", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "경도를 입력해 주세요.")
        @DecimalMin(value = "-180", message = "경도는 -180 이상 180 이하여야 합니다.")
        @DecimalMax(value = "180", message = "경도는 -180 이상 180 이하여야 합니다.")
        Double longitude
) {
}
