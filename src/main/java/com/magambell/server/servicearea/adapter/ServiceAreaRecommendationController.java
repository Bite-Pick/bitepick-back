package com.magambell.server.servicearea.adapter;

import com.magambell.server.common.ErrorResponse;
import com.magambell.server.common.Response;
import com.magambell.server.servicearea.adapter.in.web.ServiceAreaRecommendationRequest;
import com.magambell.server.servicearea.app.port.in.ServiceAreaRecommendationUseCase;
import com.magambell.server.servicearea.app.port.out.response.ServiceAreaRecommendationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ServiceArea", description = "서비스 지역 API")
@RequiredArgsConstructor
@RequestMapping("/api/v1/store/map/service-areas")
@RestController
public class ServiceAreaRecommendationController {

    private final ServiceAreaRecommendationUseCase serviceAreaRecommendationUseCase;

    @Operation(summary = "가까운 서비스 지역 추천",
            description = "마지막 지도 검색 범위의 중심 좌표를 기준으로 활성 지역의 대표 좌표까지 직선거리를 계산합니다. "
                    + "거리 오름차순으로 최대 3개를 반환하며, 동일 거리에서는 지역 ID 오름차순으로 정렬합니다. "
                    + "활성 지역이 없으면 빈 목록을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "추천 결과는 공통 응답의 data에 포함됩니다.")
    @ApiResponse(responseCode = "400", description = "좌표 누락, 형식 오류 또는 유효 범위 초과",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public Response<ServiceAreaRecommendationResponse> recommendServiceAreas(
            @ParameterObject @ModelAttribute @Validated ServiceAreaRecommendationRequest request
    ) {
        return new Response<>(serviceAreaRecommendationUseCase.recommendServiceAreas(
                request.latitude(), request.longitude()));
    }
}
