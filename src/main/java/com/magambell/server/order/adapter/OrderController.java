package com.magambell.server.order.adapter;

import com.magambell.server.common.Response;
import com.magambell.server.common.security.CustomUserDetails;
import com.magambell.server.common.swagger.BaseResponse;
import com.magambell.server.order.adapter.in.web.CreateOrderRequest;
import com.magambell.server.order.adapter.in.web.CustomerOrderListRequest;
import com.magambell.server.order.adapter.in.web.OwnerOrderListRequest;
import com.magambell.server.order.adapter.in.web.RejectOrderRequest;
import com.magambell.server.order.adapter.out.persistence.CreateOrderResponse;
import com.magambell.server.order.adapter.out.persistence.OrderDetailResponse;
import com.magambell.server.order.adapter.out.persistence.OrderListResponse;
import com.magambell.server.order.adapter.out.persistence.OrderStoreListResponse;
import com.magambell.server.order.adapter.out.persistence.OrderStoreSalesSummaryResponse;
import com.magambell.server.order.app.port.in.OrderUseCase;
import com.magambell.server.order.app.port.out.response.CreateOrderResponseDTO;
import com.magambell.server.order.app.port.out.response.OrderDetailDTO;
import com.magambell.server.order.app.port.out.response.OrderListDTO;
import com.magambell.server.order.app.port.out.response.OrderStoreListDTO;
import com.magambell.server.order.app.port.out.response.OrderStoreSalesSummaryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Order", description = "Order API")
@RequiredArgsConstructor
@RequestMapping("/api/v1/order")
@RestController
public class OrderController {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final OrderUseCase orderUseCase;

    @Operation(summary = "주문 등록")
    @ApiResponse(responseCode = "200", content = {
            @Content(schema = @Schema(implementation = CreateOrderResponse.class))})
    @PostMapping("")
    public Response<CreateOrderResponse> createOrder(
            @RequestBody @Validated final CreateOrderRequest request,
            @AuthenticationPrincipal final CustomUserDetails customUserDetails
    ) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        CreateOrderResponseDTO dto = orderUseCase.createOrder(request.toServiceRequest(), customUserDetails.userId(),
                now);
        return new Response<>(new CreateOrderResponse(dto.merchantUid(), dto.totalAmount()));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "고객 주문내역")
    @ApiResponse(responseCode = "200", content = {
            @Content(schema = @Schema(implementation = OrderListResponse.class))})
    @GetMapping("")
    public Response<OrderListResponse> getOrderList(
            @ModelAttribute @Validated final CustomerOrderListRequest request,
            @AuthenticationPrincipal final CustomUserDetails customUserDetails
    ) {
        List<OrderListDTO> orderList = orderUseCase.getOrderList(request.toService(), customUserDetails.userId());
        return new Response<>(new OrderListResponse(orderList));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "고객 주문상세")
    @ApiResponse(responseCode = "200", content = {
            @Content(schema = @Schema(implementation = OrderDetailResponse.class))})
    @GetMapping("/{orderId}")
    public Response<OrderDetailResponse> getOrderDetail(
            @PathVariable final Long orderId,
            @AuthenticationPrincipal final CustomUserDetails customUserDetails
    ) {
        OrderDetailDTO orderDetail = orderUseCase.getOrderDetail(orderId, customUserDetails.userId());
        return new Response<>(orderDetail.toResponse());
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "고객 주문 취소")
    @ApiResponse(responseCode = "200", content = {
            @Content(schema = @Schema(implementation = BaseResponse.class))})
    @PatchMapping("/cancel/{orderId}")
    public Response<BaseResponse> cancelOrder(
            @PathVariable final Long orderId,
            @AuthenticationPrincipal final CustomUserDetails customUserDetails
    ) {
        orderUseCase.cancelOrder(orderId, customUserDetails.userId());
        return new Response<>();
    }

    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "사장님 주문내역")
    @ApiResponse(responseCode = "200", content = {
            @Content(schema = @Schema(implementation = OrderStoreListResponse.class))})
    @GetMapping("/store")
    public Response<OrderStoreListResponse> getOrderStoreList(
            @ModelAttribute @Validated final OwnerOrderListRequest request,
            @AuthenticationPrincipal final CustomUserDetails customUserDetails
    ) {
        List<OrderStoreListDTO> orderStoreList = orderUseCase.getOrderStoreList(request.toService(),
                customUserDetails.userId());
        return new Response<>(new OrderStoreListResponse(orderStoreList));
    }

    @PreAuthorize("hasRole('OWNER')")
    @Operation(
            summary = "사장님 매출 요약",
            description = "결제 완료 상태를 유지하는 주문의 전체 및 이번 달 매출을 조회합니다."
    )
    @ApiResponse(responseCode = "200", content = {
            @Content(schema = @Schema(implementation = OrderStoreSalesSummaryResponse.class))})
    @GetMapping("/store/sales/summary")
    public Response<OrderStoreSalesSummaryResponse> getStoreSalesSummary(
            @AuthenticationPrincipal final CustomUserDetails customUserDetails
    ) {
        LocalDateTime calculatedAt = LocalDateTime.now(SEOUL);
        OrderStoreSalesSummaryDTO summary = orderUseCase.getStoreSalesSummary(
                customUserDetails.userId(), calculatedAt);
        return new Response<>(summary.toResponse());
    }

    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "사장님 주문 수락")
    @ApiResponse(responseCode = "200", content = {
            @Content(schema = @Schema(implementation = BaseResponse.class))})
    @PatchMapping("/approve/{orderId}")
    public Response<BaseResponse> approveOrder(
            @PathVariable final Long orderId,
            @AuthenticationPrincipal final CustomUserDetails customUserDetails
    ) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        orderUseCase.approveOrder(orderId, customUserDetails.userId(), now);
        return new Response<>();
    }

    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "사장님 주문 거절")
    @ApiResponse(responseCode = "200", content = {
            @Content(schema = @Schema(implementation = BaseResponse.class))})
    @PatchMapping("/reject/{orderId}")
    public Response<BaseResponse> rejectOrder(
            @PathVariable final Long orderId,
            @RequestBody @Validated RejectOrderRequest request,
            @AuthenticationPrincipal final CustomUserDetails customUserDetails
    ) {
        orderUseCase.rejectOrder(request.toService(orderId, customUserDetails.userId()));
        return new Response<>();
    }

    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "사장님 주문 픽업 완료")
    @ApiResponse(responseCode = "200", content = {
            @Content(schema = @Schema(implementation = BaseResponse.class))})
    @PatchMapping("/completed/{orderId}")
    public Response<BaseResponse> completedOrder(
            @PathVariable final Long orderId,
            @AuthenticationPrincipal final CustomUserDetails customUserDetails
    ) {
        orderUseCase.completedOrder(orderId, customUserDetails.userId());
        return new Response<>();
    }
}
