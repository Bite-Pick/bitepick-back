package com.magambell.server.payment.domain.repository;

import com.magambell.server.payment.app.port.out.response.StoreSalesSummaryDTO;
import com.magambell.server.payment.domain.entity.Payment;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentRepositoryCustom {
    Optional<Payment> findByMerchantUidWithLockAndRelations(String merchantUid);

    StoreSalesSummaryDTO getStoreSalesSummary(Long storeId, LocalDateTime monthStartAt,
                                              LocalDateTime calculatedAt);
}
