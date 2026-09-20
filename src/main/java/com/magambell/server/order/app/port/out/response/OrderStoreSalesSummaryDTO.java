package com.magambell.server.order.app.port.out.response;

import com.magambell.server.order.adapter.out.persistence.OrderStoreSalesSummaryResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record OrderStoreSalesSummaryDTO(
        Long totalAmount,
        Long totalOrderCount,
        Long monthlyAmount,
        Long monthlyOrderCount,
        LocalDate monthStartDate,
        LocalDateTime firstSoldAt,
        LocalDateTime calculatedAt
) {

    public OrderStoreSalesSummaryResponse toResponse() {
        return new OrderStoreSalesSummaryResponse(
                totalAmount,
                totalOrderCount,
                monthlyAmount,
                monthlyOrderCount,
                monthStartDate,
                firstSoldAt,
                calculatedAt
        );
    }
}
