package com.magambell.server.servicearea.adapter;

import com.magambell.server.common.Response;
import com.magambell.server.servicearea.adapter.in.web.ServiceAreaResponse;
import com.magambell.server.servicearea.domain.repository.ServiceAreaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "ServiceArea", description = "서비스 지역 API")
@RequiredArgsConstructor
@RequestMapping("/api/v1/service-areas")
@RestController
public class ServiceAreaController {

    private final ServiceAreaRepository serviceAreaRepository;

    @Operation(summary = "서비스 중인 지역 목록 조회", description = "현재 서비스 중인 모든 지역의 목록을 반환합니다.")
    @GetMapping
    public Response<List<ServiceAreaResponse>> getServiceAreas() {
        List<ServiceAreaResponse> serviceAreas = serviceAreaRepository.findByActiveTrue()
                .stream()
                .map(ServiceAreaResponse::from)
                .toList();
        
        return new Response<>(serviceAreas);
    }
}
