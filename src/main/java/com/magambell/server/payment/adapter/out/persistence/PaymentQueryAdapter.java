package com.magambell.server.payment.adapter.out.persistence;

import com.magambell.server.common.annotation.Adapter;
import com.magambell.server.common.enums.ErrorCode;
import com.magambell.server.common.exception.NotFoundException;
import com.magambell.server.payment.app.port.out.PaymentQueryPort;
import com.magambell.server.payment.app.port.out.response.StoreSalesSummaryDTO;
import com.magambell.server.payment.domain.entity.Payment;
import com.magambell.server.payment.domain.repository.PaymentRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Adapter
public class PaymentQueryAdapter implements PaymentQueryPort {
    private final PaymentRepository paymentRepository;

    @Override
    public Payment findByMerchantUidWithLockAndRelations(final String merchantUid) {
        return paymentRepository.findByMerchantUidWithLockAndRelations(merchantUid)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    @Override
    public StoreSalesSummaryDTO getStoreSalesSummary(final Long storeId, final LocalDateTime monthStartAt,
                                                     final LocalDateTime calculatedAt) {
        return paymentRepository.getStoreSalesSummary(storeId, monthStartAt, calculatedAt);
    }
}
