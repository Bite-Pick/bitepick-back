package com.magambell.server.order.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.magambell.server.common.Response;
import com.magambell.server.common.security.CustomUserDetails;
import com.magambell.server.order.adapter.out.persistence.OrderStoreSalesSummaryResponse;
import com.magambell.server.order.app.port.in.OrderUseCase;
import com.magambell.server.order.app.port.out.response.OrderStoreSalesSummaryDTO;
import com.magambell.server.user.domain.enums.UserRole;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

@ExtendWith(MockitoExtension.class)
class OrderSalesSummaryControllerTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Mock
    private OrderUseCase orderUseCase;

    @InjectMocks
    private OrderController orderController;

    @DisplayName("사장님 매출 요약 API는 서울 시간 기준 집계 결과를 반환한다")
    @Test
    void getStoreSalesSummary() {
        // given
        Long userId = 1L;
        CustomUserDetails userDetails = new CustomUserDetails(userId, UserRole.OWNER);
        LocalDateTime firstSoldAt = LocalDateTime.of(2026, 8, 20, 11, 0);
        LocalDateTime beforeCall = LocalDateTime.now(SEOUL);
        given(orderUseCase.getStoreSalesSummary(eq(userId), any(LocalDateTime.class)))
                .willAnswer(invocation -> {
                    LocalDateTime calculatedAt = invocation.getArgument(1);
                    return new OrderStoreSalesSummaryDTO(
                            87_000L,
                            10L,
                            30_000L,
                            3L,
                            calculatedAt.toLocalDate().withDayOfMonth(1),
                            firstSoldAt,
                            calculatedAt
                    );
                });

        // when
        Response<OrderStoreSalesSummaryResponse> response = orderController.getStoreSalesSummary(userDetails);
        LocalDateTime afterCall = LocalDateTime.now(SEOUL);

        // then
        OrderStoreSalesSummaryResponse data = response.getData();
        assertThat(data.totalAmount()).isEqualTo(87_000L);
        assertThat(data.totalOrderCount()).isEqualTo(10L);
        assertThat(data.monthlyAmount()).isEqualTo(30_000L);
        assertThat(data.monthlyOrderCount()).isEqualTo(3L);
        assertThat(data.monthStartDate()).isEqualTo(data.calculatedAt().toLocalDate().withDayOfMonth(1));
        assertThat(data.firstSoldAt()).isEqualTo(firstSoldAt);
        assertThat(data.calculatedAt()).isBetween(beforeCall, afterCall);

        ArgumentCaptor<LocalDateTime> calculatedAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderUseCase).getStoreSalesSummary(eq(userId), calculatedAtCaptor.capture());
        assertThat(calculatedAtCaptor.getValue()).isBetween(beforeCall, afterCall);
    }

    @DisplayName("사장님 매출 요약 API는 OWNER 권한과 지정된 경로를 사용한다")
    @Test
    void getStoreSalesSummaryHasOwnerAuthorizationAndPath() throws NoSuchMethodException {
        // given
        Method method = OrderController.class.getMethod("getStoreSalesSummary", CustomUserDetails.class);

        // when
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);

        // then
        assertThat(preAuthorize.value()).isEqualTo("hasRole('OWNER')");
        assertThat(getMapping.value()).containsExactly("/store/sales/summary");
    }
}
