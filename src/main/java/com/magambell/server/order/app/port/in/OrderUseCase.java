package com.magambell.server.order.app.port.in;

import com.magambell.server.order.app.port.in.request.CreateOrderServiceRequest;
import com.magambell.server.order.app.port.in.request.CustomerOrderListServiceRequest;
import com.magambell.server.order.app.port.in.request.OwnerOrderListServiceRequest;
import com.magambell.server.order.app.port.in.request.RejectOrderServiceRequest;
import com.magambell.server.order.app.port.out.response.CreateOrderResponseDTO;
import com.magambell.server.order.app.port.out.response.OrderDetailDTO;
import com.magambell.server.order.app.port.out.response.OrderListDTO;
import com.magambell.server.order.app.port.out.response.OrderStoreSalesSummaryDTO;
import com.magambell.server.order.app.port.out.response.OrderStoreListDTO;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderUseCase {
    CreateOrderResponseDTO createOrder(CreateOrderServiceRequest serviceRequest, Long userId, LocalDateTime now);

    List<OrderListDTO> getOrderList(CustomerOrderListServiceRequest request, Long userId);

    OrderDetailDTO getOrderDetail(Long orderId, Long userId);

    List<OrderStoreListDTO> getOrderStoreList(OwnerOrderListServiceRequest request, Long userId);

    OrderStoreSalesSummaryDTO getStoreSalesSummary(Long userId, LocalDateTime calculatedAt);

    void approveOrder(Long orderId, Long userId, LocalDateTime now);

    void rejectOrder(RejectOrderServiceRequest request);

    void cancelOrder(Long orderId, Long userId);

    void completedOrder(Long orderId, Long userId);

    void batchRejectOrdersBeforePickup(LocalDateTime pickupTime, LocalDateTime createdAtCutOff);

    void autoRejectOrdersAfter(LocalDateTime minusMinutes, LocalDateTime pickupTime);
}
