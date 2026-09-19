package com.magambell.server.order.app.service;

import static com.magambell.server.payment.domain.enums.PaymentStatus.READY;

import com.magambell.server.common.enums.ErrorCode;
import com.magambell.server.common.exception.InvalidRequestException;
import com.magambell.server.common.exception.NotFoundException;
import com.magambell.server.common.exception.UnauthorizedException;
import com.magambell.server.goods.app.port.out.GoodsQueryPort;
import com.magambell.server.goods.domain.entity.Goods;
import com.magambell.server.notification.app.port.in.NotificationUseCase;
import com.magambell.server.order.app.port.in.OrderUseCase;
import com.magambell.server.order.app.port.in.request.CreateOrderServiceRequest;
import com.magambell.server.order.app.port.in.request.CustomerOrderListServiceRequest;
import com.magambell.server.order.app.port.in.request.OwnerOrderListServiceRequest;
import com.magambell.server.order.app.port.in.request.RejectOrderServiceRequest;
import com.magambell.server.order.app.port.out.OrderCommandPort;
import com.magambell.server.order.app.port.out.OrderQueryPort;
import com.magambell.server.order.app.port.out.response.CreateOrderResponseDTO;
import java.time.LocalTime;
import com.magambell.server.order.app.port.out.response.OrderDetailDTO;
import com.magambell.server.order.app.port.out.response.OrderListDTO;
import com.magambell.server.order.app.port.out.response.OrderStoreSalesSummaryDTO;
import com.magambell.server.order.app.port.out.response.OrderStoreListDTO;
import com.magambell.server.order.domain.entity.Order;
import com.magambell.server.order.domain.enums.OrderStatus;
import com.magambell.server.order.domain.enums.RejectReason;
import com.magambell.server.payment.app.port.in.dto.CreatePaymentDTO;
import com.magambell.server.payment.app.port.out.PaymentCommandPort;
import com.magambell.server.payment.app.port.out.PaymentQueryPort;
import com.magambell.server.payment.app.port.out.PortOnePort;
import com.magambell.server.payment.app.port.out.response.StoreSalesSummaryDTO;
import com.magambell.server.payment.domain.entity.Payment;
import com.magambell.server.payment.domain.enums.PaymentCompletionType;
import com.magambell.server.stock.app.port.in.StockUseCase;
import com.magambell.server.stock.app.port.out.StockCommandPort;
import com.magambell.server.stock.app.port.out.StockQueryPort;
import com.magambell.server.stock.domain.entity.Stock;
import com.magambell.server.stock.domain.entity.StockHistory;
import com.magambell.server.store.app.port.out.StoreQueryPort;
import com.magambell.server.store.domain.entity.Store;
import com.magambell.server.user.app.port.out.UserQueryPort;
import com.magambell.server.user.domain.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class OrderService implements OrderUseCase {

    private static final List<OrderStatus> OWNER_LIST_VALID_STATUSES = List.of(
            OrderStatus.PENDING,
            OrderStatus.PAID,
            OrderStatus.ACCEPTED,
            OrderStatus.COMPLETED,
            OrderStatus.CANCELED
    );

    private final OrderCommandPort orderCommandPort;
    private final OrderQueryPort orderQueryPort;
    private final GoodsQueryPort goodsQueryPort;
    private final UserQueryPort userQueryPort;
    private final StockCommandPort stockCommandPort;
    private final PaymentCommandPort paymentCommandPort;
    private final StockQueryPort stockQueryPort;
    private final StockUseCase stockUseCase;
    private final PaymentQueryPort paymentQueryPort;
    private final StoreQueryPort storeQueryPort;
    private final PortOnePort portOnePort;
    private final NotificationUseCase notificationUseCase;

    @Transactional
    @Override
    public CreateOrderResponseDTO createOrder(final CreateOrderServiceRequest request, final Long userId,
                                              final LocalDateTime now) {
        User user = userQueryPort.findById(userId);
        Goods goods = goodsQueryPort.findById(request.goodsId());

        validateOrderRequest(request, goods, now);

        Order order = orderCommandPort.createOrder(request.toDTO(user, goods));

        Payment payment = paymentCommandPort.createReadyPayment(
                new CreatePaymentDTO(order,
                        request.totalPrice(),
                        READY)
        );

        return new CreateOrderResponseDTO(payment.getMerchantUid(), payment.getAmount());
    }

    @Override
    public List<OrderListDTO> getOrderList(final CustomerOrderListServiceRequest request, final Long userId) {
        User user = userQueryPort.findById(userId);
        return orderQueryPort.getOrderList(PageRequest.of(request.page() - 1, request.size()), user.getId());
    }

    @Override
    public OrderDetailDTO getOrderDetail(final Long orderId, final Long userId) {
        User user = userQueryPort.findById(userId);
        return orderQueryPort.getOrderDetail(orderId, user.getId());
    }

    @Override
    public List<OrderStoreListDTO> getOrderStoreList(final OwnerOrderListServiceRequest request, final Long userId) {
        if (request.orderStatus() != null && !OWNER_LIST_VALID_STATUSES.contains(request.orderStatus())) {
            throw new InvalidRequestException(ErrorCode.INVALID_ORDER_STATUS);
        }
        User user = userQueryPort.findById(userId);
        return orderQueryPort.getOrderStoreList(PageRequest.of(request.page() - 1, request.size()), user.getId()
                , request.orderStatus());
    }

    @Override
    public OrderStoreSalesSummaryDTO getStoreSalesSummary(final Long userId, final LocalDateTime calculatedAt) {
        User user = userQueryPort.findById(userId);
        Store store = storeQueryPort.getStoreByUser(user)
                .orElseThrow(() -> new NotFoundException(ErrorCode.STORE_NOT_FOUND));
        LocalDate monthStartDate = calculatedAt.toLocalDate().withDayOfMonth(1);
        StoreSalesSummaryDTO summary = paymentQueryPort.getStoreSalesSummary(
                store.getId(), monthStartDate.atStartOfDay(), calculatedAt);

        return new OrderStoreSalesSummaryDTO(
                summary.totalAmount(),
                summary.totalOrderCount(),
                summary.monthlyAmount(),
                summary.monthlyOrderCount(),
                monthStartDate,
                summary.firstSoldAt(),
                calculatedAt
        );
    }

    @Transactional
    @Override
    public void approveOrder(final Long orderId, final Long userId, final LocalDateTime now) {
        User user = userQueryPort.findById(userId);
        Order order = orderQueryPort.findOwnerWithAllById(orderId);
        validateApproveOrder(user, order, now);
        order.accepted();

        notificationUseCase.notifyApproveOrder(order.getUser(), order.getPickupTime());
    }

    @Transactional
    @Override
    public void rejectOrder(final RejectOrderServiceRequest request) {
        User user = userQueryPort.findById(request.userId());
        Order order = orderQueryPort.findOwnerWithAllById(request.orderId());

        validateRejectOrder(user, order);
        order.rejected(request.rejectReason());

        Payment payment = order.getPayment();
        stockUseCase.restoreStockIfNecessary(payment);
        
        // 실제 결제가 있는 경우에만 PortOne 환불 처리
        if (payment.getTransactionId() != null && !payment.getTransactionId().startsWith("test_")) {
            try {
            portOnePort.cancelPayment(payment.getMerchantUid(), order.getTotalPrice(), "사장님 주문 취소");
            log.info("PortOne 결제 취소 성공: paymentId={}, transactionId={}, orderId={}", 
                payment.getMerchantUid(), payment.getTransactionId(), request.orderId());
            } catch (NotFoundException e) {
                // PortOne에 결제 정보가 없는 경우 (이미 취소됨 또는 결제 미완료)
                // 주문 취소는 정상적으로 진행
            log.warn("PortOne에서 결제를 찾을 수 없음 - 주문 취소는 정상 처리: paymentId={}, transactionId={}, orderId={}", 
                payment.getMerchantUid(), payment.getTransactionId(), request.orderId());
            } catch (Exception e) {
                // 실제 네트워크 에러, 인증 에러 등 - 재시도 필요
            log.error("PortOne 결제 취소 중 예상치 못한 에러 발생 - 수동 환불 필요: paymentId={}, transactionId={}, orderId={}", 
                payment.getMerchantUid(), payment.getTransactionId(), request.orderId(), e);
                throw e; // 예외를 다시 던져서 트랜잭션 롤백
            }
        } else {
            log.info("Test payment detected - skipping PortOne cancellation for transactionId: {}", 
                    payment.getTransactionId());
        }
        
        notificationUseCase.notifyRejectOrder(order.getUser());
    }

    @Transactional
    @Override
    public void cancelOrder(final Long orderId, final Long userId) {
        User user = userQueryPort.findById(userId);
        Order order = orderQueryPort.findWithAllById(orderId);

        validateCancelOrder(user, order);
        
        Payment payment = order.getPayment();
        if (payment == null) {
            log.warn("Payment not found for order: {}", orderId);
            return;
        }
        
        // PENDING 상태 (결제 미완료) 주문은 결제 처리 없이 취소만 진행
        if (order.getOrderStatus() == OrderStatus.PENDING) {
            log.info("결제 미완료 주문 취소 - orderId: {}, status: PENDING", orderId);
            order.cancelled(PaymentCompletionType.REDIRECT);
            return;
        }
        
        // PAID 이상의 상태: 재고 복원 및 결제 취소 처리
        order.cancelled(PaymentCompletionType.REDIRECT);
        stockUseCase.restoreStockIfNecessary(payment);
        
        // 실제 결제가 있는 경우에만 PortOne 환불 처리
        if (payment.getTransactionId() != null && !payment.getTransactionId().startsWith("test_")) {
            try {
            portOnePort.cancelPayment(payment.getMerchantUid(), order.getTotalPrice(), "고객님 주문 취소");
            log.info("PortOne 결제 취소 성공: paymentId={}, transactionId={}, orderId={}", 
                payment.getMerchantUid(), payment.getTransactionId(), orderId);
            } catch (NotFoundException e) {
                // PortOne에 결제 정보가 없는 경우 (이미 취소됨 또는 결제 미완료)
                // 주문 취소는 정상적으로 진행
            log.warn("PortOne에서 결제를 찾을 수 없음 - 주문 취소는 정상 처리: paymentId={}, transactionId={}, orderId={}", 
                payment.getMerchantUid(), payment.getTransactionId(), orderId);
            } catch (Exception e) {
                // 실제 네트워크 에러, 인증 에러 등 - 재시도 필요
            log.error("PortOne 결제 취소 중 예상치 못한 에러 발생 - 수동 환불 필요: paymentId={}, transactionId={}, orderId={}", 
                payment.getMerchantUid(), payment.getTransactionId(), orderId, e);
                throw e; // 예외를 다시 던져서 트랜잭션 롤백
            }
        } else {
            log.info("Test payment detected - skipping PortOne cancellation for transactionId: {}", 
                    payment.getTransactionId());
        }
    }

    @Transactional
    @Override
    public void completedOrder(final Long orderId, final Long userId) {
        User user = userQueryPort.findById(userId);
        Order order = orderQueryPort.findOwnerWithAllById(orderId);

        validateCompletedOrder(user, order);
        order.completed();
    }

    @Transactional
    @Override
    public void batchRejectOrdersBeforePickup(final LocalDateTime pickupTime, final LocalDateTime createdAtCutOff) {
        List<Order> orders = orderQueryPort.findByPaidBeforePickupRejectProcessedOrders(pickupTime, createdAtCutOff);
        orders.forEach(order -> {
            log.info("[픽업 30분 전] 시스템 주문 거절 order = {}", order.getId());
            order.rejected(RejectReason.SYSTEM);
            Payment payment = order.getPayment();
            stockUseCase.restoreStockIfNecessary(payment);
            
            // 실제 결제가 있는 경우에만 PortOne 환불 처리
            if (payment.getTransactionId() != null && !payment.getTransactionId().startsWith("test_")) {
                try {
                    portOnePort.cancelPayment(payment.getMerchantUid(), order.getTotalPrice(), "시스템 주문 취소");
                    log.info("[스케줄러] PortOne 결제 취소 성공: paymentId={}, transactionId={}, orderId={}", 
                        payment.getMerchantUid(), payment.getTransactionId(), order.getId());
                } catch (NotFoundException e) {
                    // PortOne에 결제 정보가 없는 경우 - 주문 거절은 정상 진행
                    log.warn("[스케줄러] PortOne에서 결제를 찾을 수 없음 - 주문 거절은 정상 처리: paymentId={}, transactionId={}, orderId={}", 
                        payment.getMerchantUid(), payment.getTransactionId(), order.getId());
                } catch (Exception e) {
                    // 기타 에러는 로그만 남기고 다음 주문 처리 계속 (트랜잭션 롤백 방지)
                    log.error("[스케줄러] PortOne 결제 취소 실패 - 주문 거절은 완료, 수동 환불 필요: paymentId={}, transactionId={}, orderId={}", 
                        payment.getMerchantUid(), payment.getTransactionId(), order.getId(), e);
                }
            } else {
                log.info("[스케줄러] Test payment detected - skipping PortOne cancellation for transactionId: {}", 
                        payment.getTransactionId());
            }
            
            notificationUseCase.notifyRejectOrder(order.getUser());
        });
    }

    @Transactional
    @Override
    public void autoRejectOrdersAfter(final LocalDateTime minusMinutes, final LocalDateTime pickupTime) {
        List<Order> orders = orderQueryPort.findByAutoRejectProcessedOrders(minusMinutes,
                pickupTime);
        orders.forEach(order -> {
            log.info("[5분 마다] 시스템 주문 거절 order = {}", order.getId());
            order.rejected(RejectReason.SYSTEM);
            Payment payment = order.getPayment();
            stockUseCase.restoreStockIfNecessary(payment);
            
            // 실제 결제가 있는 경우에만 PortOne 환불 처리
            if (payment.getTransactionId() != null && !payment.getTransactionId().startsWith("test_")) {
                try {
                    portOnePort.cancelPayment(payment.getMerchantUid(), order.getTotalPrice(), "시스템 주문 취소");
                    log.info("[스케줄러] PortOne 결제 취소 성공: paymentId={}, transactionId={}, orderId={}", 
                        payment.getMerchantUid(), payment.getTransactionId(), order.getId());
                } catch (NotFoundException e) {
                    // PortOne에 결제 정보가 없는 경우 - 주문 거절은 정상 진행
                    log.warn("[스케줄러] PortOne에서 결제를 찾을 수 없음 - 주문 거절은 정상 처리: paymentId={}, transactionId={}, orderId={}", 
                        payment.getMerchantUid(), payment.getTransactionId(), order.getId());
                } catch (Exception e) {
                    // 기타 에러는 로그만 남기고 다음 주문 처리 계속 (트랜잭션 롤백 방지)
                    log.error("[스케줄러] PortOne 결제 취소 실패 - 주문 거절은 완료, 수동 환불 필요: paymentId={}, transactionId={}, orderId={}", 
                        payment.getMerchantUid(), payment.getTransactionId(), order.getId(), e);
                }
            } else {
                log.info("[스케줄러] Test payment detected - skipping PortOne cancellation for transactionId: {}", 
                        payment.getTransactionId());
            }
            
            notificationUseCase.notifyRejectOrder(order.getUser());
        });
    }

    private void validateOrderRequest(final CreateOrderServiceRequest request, final Goods goods,
                                      final LocalDateTime now) {
        if (request.totalPrice() != goods.getSalePrice() * request.quantity()) {
            throw new InvalidRequestException(ErrorCode.INVALID_TOTAL_PRICE);
        }

        // 픽업 시간의 시간 부분만 추출하여 상품의 판매 시간대와 비교
        LocalTime pickupLocalTime = request.pickupTime().toLocalTime();
        LocalTime goodsStartTime = goods.getStartTime().toLocalTime();
        LocalTime goodsEndTime = goods.getEndTime().toLocalTime();
        
        // 자정을 넘는 시간대 처리 (예: 23:00 ~ 02:00)
        boolean isValidTime;
        if (goodsEndTime.isBefore(goodsStartTime)) {
            // 자정을 넘는 경우: 시작 시간 이후이거나 종료 시간 이전이면 유효
            isValidTime = !pickupLocalTime.isBefore(goodsStartTime) || !pickupLocalTime.isAfter(goodsEndTime);
        } else {
            // 일반적인 경우: 시작과 종료 사이에 있어야 함
            isValidTime = !pickupLocalTime.isBefore(goodsStartTime) && !pickupLocalTime.isAfter(goodsEndTime);
        }
        
        if (!isValidTime) {
            throw new InvalidRequestException(ErrorCode.INVALID_PICKUP_TIME);
        }

        if (!request.pickupTime().isAfter(now)) {
            throw new InvalidRequestException(ErrorCode.INVALID_NOW_TIME_PICKUP_TIME);
        }
    }

    private void validateApproveOrder(final User user, final Order order, final LocalDateTime now) {
        if (!order.isOwner(user)) {
            throw new UnauthorizedException(ErrorCode.ORDER_NO_ACCESS);
        }

        validateOrderForDecision(order);

        if (order.getPickupTime().isBefore(now)) {
            throw new InvalidRequestException(ErrorCode.INVALID_PICKUP_TIME);
        }
    }

    private void validateRejectOrder(final User user, final Order order) {
        if (!order.isOwner(user)) {
            throw new UnauthorizedException(ErrorCode.ORDER_NO_ACCESS);
        }

        validateOrderForDecision(order);
    }

    private void validateCancelOrder(final User user, final Order order) {
        if (!Objects.equals(order.getUser().getId(), user.getId())) {
            throw new UnauthorizedException(ErrorCode.ORDER_NO_ACCESS);
        }

        log.info("주문 취소 검증 - orderId: {}, currentStatus: {}", order.getId(), order.getOrderStatus());
        
        // PENDING 상태는 결제 전이므로 즉시 취소 가능
        if (order.getOrderStatus() == OrderStatus.PENDING) {
            return;
        }
        
        // PENDING이 아닌 경우 기존 검증 로직 적용 (PAID만 취소 가능)
        validateOrderForDecision(order);
    }

    private void validateCompletedOrder(final User user, final Order order) {
        if (!order.isOwner(user)) {
            throw new UnauthorizedException(ErrorCode.ORDER_NO_ACCESS);
        }

        if (order.getOrderStatus() != OrderStatus.ACCEPTED) {
            throw new InvalidRequestException(ErrorCode.ORDER_NOT_APPROVED);
        }
    }

    private void validateOrderForDecision(final Order order) {
        log.info("주문 상태 검증 - orderId: {}, currentStatus: {}", order.getId(), order.getOrderStatus());
        
        if (order.getOrderStatus() == OrderStatus.ACCEPTED) {
            log.warn("이미 승인된 주문 - orderId: {}", order.getId());
            throw new InvalidRequestException(ErrorCode.ORDER_ALREADY_ACCEPTED);
        }

        if (order.getOrderStatus() == OrderStatus.REJECTED) {
            log.warn("이미 거절된 주문 - orderId: {}", order.getId());
            throw new InvalidRequestException(ErrorCode.ORDER_ALREADY_REJECTED);
        }

        if (order.getOrderStatus() != OrderStatus.PAID) {
            log.error("결제 완료되지 않은 주문 - orderId: {}, status: {}", order.getId(), order.getOrderStatus());
            throw new InvalidRequestException(ErrorCode.INVALID_PAYMENT_STATUS_PAID);
        }
    }

}
