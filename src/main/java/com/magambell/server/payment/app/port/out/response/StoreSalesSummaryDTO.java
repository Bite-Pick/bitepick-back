package com.magambell.server.payment.app.port.out.response;

import java.time.LocalDateTime;

public record StoreSalesSummaryDTO(
        Long totalAmount,
        Long totalOrderCount,
        Long monthlyAmount,
        Long monthlyOrderCount,
        LocalDateTime firstSoldAt
) {
}
