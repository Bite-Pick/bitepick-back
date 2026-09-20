package com.magambell.server.order.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.magambell.server.common.enums.ErrorCode;
import com.magambell.server.common.exception.NotFoundException;
import com.magambell.server.goods.app.port.out.GoodsQueryPort;
import com.magambell.server.notification.app.port.in.NotificationUseCase;
import com.magambell.server.order.app.port.out.OrderCommandPort;
import com.magambell.server.order.app.port.out.OrderQueryPort;
import com.magambell.server.order.app.port.out.response.OrderStoreSalesSummaryDTO;
import com.magambell.server.payment.app.port.out.PaymentCommandPort;
import com.magambell.server.payment.app.port.out.PaymentQueryPort;
import com.magambell.server.payment.app.port.out.PortOnePort;
import com.magambell.server.payment.app.port.out.response.StoreSalesSummaryDTO;
import com.magambell.server.stock.app.port.in.StockUseCase;
import com.magambell.server.stock.app.port.out.StockCommandPort;
import com.magambell.server.stock.app.port.out.StockQueryPort;
import com.magambell.server.store.app.port.out.StoreQueryPort;
import com.magambell.server.store.domain.entity.Store;
import com.magambell.server.user.app.port.out.UserQueryPort;
import com.magambell.server.user.domain.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderSalesSummaryServiceTest {

    @Mock
    private OrderCommandPort orderCommandPort;
    @Mock
    private OrderQueryPort orderQueryPort;
    @Mock
    private GoodsQueryPort goodsQueryPort;
    @Mock
    private UserQueryPort userQueryPort;
    @Mock
    private StockCommandPort stockCommandPort;
    @Mock
    private PaymentCommandPort paymentCommandPort;
    @Mock
    private StockQueryPort stockQueryPort;
    @Mock
    private StockUseCase stockUseCase;
    @Mock
    private PaymentQueryPort paymentQueryPort;
    @Mock
    private StoreQueryPort storeQueryPort;
    @Mock
    private PortOnePort portOnePort;
    @Mock
    private NotificationUseCase notificationUseCase;

    @InjectMocks
    private OrderService orderService;

    @DisplayName("사장님 매출 요약은 서울 시간 기준 월 시작일과 집계 시각을 포함한다")
    @Test
    void getStoreSalesSummary() {
        // given
        Long userId = 1L;
        Long storeId = 10L;
        LocalDateTime calculatedAt = LocalDateTime.of(2026, 9, 19, 14, 30);
        LocalDateTime monthStartAt = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime firstSoldAt = LocalDateTime.of(2026, 8, 20, 11, 0);
        User user = mock(User.class);
        Store store = mock(Store.class);
        StoreSalesSummaryDTO repositorySummary = new StoreSalesSummaryDTO(
                87_000L,
                10L,
                30_000L,
                3L,
                firstSoldAt
        );

        given(userQueryPort.findById(userId)).willReturn(user);
        given(storeQueryPort.getStoreByUser(user)).willReturn(Optional.of(store));
        given(store.getId()).willReturn(storeId);
        given(paymentQueryPort.getStoreSalesSummary(storeId, monthStartAt, calculatedAt))
                .willReturn(repositorySummary);

        // when
        OrderStoreSalesSummaryDTO result = orderService.getStoreSalesSummary(userId, calculatedAt);

        // then
        assertThat(result.totalAmount()).isEqualTo(87_000L);
        assertThat(result.totalOrderCount()).isEqualTo(10L);
        assertThat(result.monthlyAmount()).isEqualTo(30_000L);
        assertThat(result.monthlyOrderCount()).isEqualTo(3L);
        assertThat(result.monthStartDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(result.firstSoldAt()).isEqualTo(firstSoldAt);
        assertThat(result.calculatedAt()).isEqualTo(calculatedAt);
        assertThat(result.toResponse().totalAmount()).isEqualTo(87_000L);
        verify(paymentQueryPort).getStoreSalesSummary(storeId, monthStartAt, calculatedAt);
    }

    @DisplayName("매출이 없는 매장은 0원과 0건, null 최초 판매 일시를 반환한다")
    @Test
    void getStoreSalesSummaryReturnsEmptySummary() {
        // given
        Long userId = 1L;
        Long storeId = 10L;
        LocalDateTime calculatedAt = LocalDateTime.of(2026, 9, 19, 14, 30);
        User user = mock(User.class);
        Store store = mock(Store.class);

        given(userQueryPort.findById(userId)).willReturn(user);
        given(storeQueryPort.getStoreByUser(user)).willReturn(Optional.of(store));
        given(store.getId()).willReturn(storeId);
        given(paymentQueryPort.getStoreSalesSummary(
                storeId, LocalDateTime.of(2026, 9, 1, 0, 0), calculatedAt))
                .willReturn(new StoreSalesSummaryDTO(0L, 0L, 0L, 0L, null));

        // when
        OrderStoreSalesSummaryDTO result = orderService.getStoreSalesSummary(userId, calculatedAt);

        // then
        assertThat(result.totalAmount()).isZero();
        assertThat(result.totalOrderCount()).isZero();
        assertThat(result.monthlyAmount()).isZero();
        assertThat(result.monthlyOrderCount()).isZero();
        assertThat(result.firstSoldAt()).isNull();
    }

    @DisplayName("사장님에게 등록된 매장이 없으면 매장 없음 예외가 발생한다")
    @Test
    void getStoreSalesSummaryFailsWhenStoreDoesNotExist() {
        // given
        Long userId = 1L;
        User user = mock(User.class);
        given(userQueryPort.findById(userId)).willReturn(user);
        given(storeQueryPort.getStoreByUser(user)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> orderService.getStoreSalesSummary(
                userId, LocalDateTime.of(2026, 9, 19, 14, 30)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(ErrorCode.STORE_NOT_FOUND.getMessage());
    }
}
