package com.magambell.server.payment.app.port.out;

import com.magambell.server.payment.app.port.out.response.StoreSalesSummaryDTO;
import com.magambell.server.payment.domain.entity.Payment;
import java.time.LocalDateTime;

public interface PaymentQueryPort {
    Payment findByMerchantUidWithLockAndRelations(String merchantUid);

    StoreSalesSummaryDTO getStoreSalesSummary(Long storeId, LocalDateTime monthStartAt,
                                              LocalDateTime calculatedAt);
}
