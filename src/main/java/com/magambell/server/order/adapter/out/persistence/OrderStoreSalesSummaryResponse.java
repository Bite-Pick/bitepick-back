package com.magambell.server.order.adapter.out.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OrderStoreSalesSummaryResponse(
        Long totalAmount,
        Long totalOrderCount,
        Long monthlyAmount,
        Long monthlyOrderCount,
        LocalDate monthStartDate,
        LocalDateTime firstSoldAt,
        LocalDateTime calculatedAt
) {
}
