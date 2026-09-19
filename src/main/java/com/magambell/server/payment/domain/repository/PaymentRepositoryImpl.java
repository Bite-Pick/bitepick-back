package com.magambell.server.payment.domain.repository;

import static com.magambell.server.goods.domain.entity.QGoods.goods;
import static com.magambell.server.order.domain.entity.QOrder.order;
import static com.magambell.server.order.domain.entity.QOrderGoods.orderGoods;
import static com.magambell.server.payment.domain.entity.QPayment.payment;
import static com.magambell.server.payment.domain.enums.PaymentStatus.PAID;
import static com.magambell.server.stock.domain.entity.QStock.stock;
import static com.magambell.server.store.domain.entity.QStore.store;
import static com.magambell.server.user.domain.entity.QUser.user;

import com.magambell.server.payment.app.port.out.response.StoreSalesSummaryDTO;
import com.magambell.server.order.domain.enums.OrderStatus;
import com.magambell.server.payment.domain.entity.Payment;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepositoryCustom {

    private static final List<OrderStatus> SALES_ORDER_STATUSES =
            List.of(OrderStatus.PAID, OrderStatus.ACCEPTED, OrderStatus.COMPLETED);

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Payment> findByMerchantUidWithLockAndRelations(final String merchantUid) {
        Payment lockedPayment = queryFactory.selectFrom(payment)
                .where(payment.merchantUid.eq(merchantUid))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne();

        if (lockedPayment == null) {
            return Optional.empty();
        }

        queryFactory.selectFrom(payment)
                .join(payment.order, order).fetchJoin()
                .join(order.orderGoodsList, orderGoods).fetchJoin()
                .join(orderGoods.goods, goods).fetchJoin()
                .join(goods.stock, stock).fetchJoin()
                .join(goods.store, store).fetchJoin()
                .join(store.user, user).fetchJoin()
                .where(payment.merchantUid.eq(merchantUid))
                .fetchOne();

        return Optional.of(lockedPayment);
    }

    @Override
    public StoreSalesSummaryDTO getStoreSalesSummary(final Long storeId, final LocalDateTime monthStartAt,
                                                     final LocalDateTime calculatedAt) {
        NumberExpression<Long> amountSum = payment.amount.castToNum(Long.class).sum();
        NumberExpression<Long> orderCount = payment.id.count();
        DateTimeExpression<LocalDateTime> firstSoldAt = payment.paidAt.min();

        Tuple totalResult = queryFactory
                .select(
                        amountSum,
                        orderCount,
                        firstSoldAt
                )
                .from(payment)
                .join(payment.order, order)
                .where(salesConditions(storeId, calculatedAt))
                .fetchOne();

        Tuple monthlyResult = queryFactory
                .select(amountSum, orderCount)
                .from(payment)
                .join(payment.order, order)
                .where(
                        salesConditions(storeId, calculatedAt),
                        payment.paidAt.goe(monthStartAt)
                )
                .fetchOne();

        if (totalResult == null || monthlyResult == null) {
            return emptySalesSummary();
        }

        return new StoreSalesSummaryDTO(
                valueOrZero(totalResult.get(amountSum)),
                valueOrZero(totalResult.get(orderCount)),
                valueOrZero(monthlyResult.get(amountSum)),
                valueOrZero(monthlyResult.get(orderCount)),
                totalResult.get(firstSoldAt)
        );
    }

    private BooleanBuilder salesConditions(final Long storeId, final LocalDateTime calculatedAt) {
        return new BooleanBuilder()
                .and(payment.paymentStatus.eq(PAID))
                .and(order.orderStatus.in(SALES_ORDER_STATUSES))
                .and(payment.paidAt.isNotNull())
                .and(payment.paidAt.loe(calculatedAt))
                .and(JPAExpressions.selectOne()
                        .from(orderGoods)
                        .join(orderGoods.goods, goods)
                        .where(
                                orderGoods.order.eq(order),
                                goods.store.id.eq(storeId)
                        )
                        .exists());
    }

    private StoreSalesSummaryDTO emptySalesSummary() {
        return new StoreSalesSummaryDTO(0L, 0L, 0L, 0L, null);
    }

    private Long valueOrZero(final Long value) {
        return Objects.requireNonNullElse(value, 0L);
    }
}
