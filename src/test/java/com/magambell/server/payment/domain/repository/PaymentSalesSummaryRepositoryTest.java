package com.magambell.server.payment.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.magambell.server.auth.domain.ProviderType;
import com.magambell.server.goods.adapter.in.web.GoodsImagesRegister;
import com.magambell.server.goods.app.port.in.dto.RegisterGoodsDTO;
import com.magambell.server.goods.domain.entity.Goods;
import com.magambell.server.goods.domain.repository.GoodsRepository;
import com.magambell.server.order.app.port.in.dto.CreateOrderDTO;
import com.magambell.server.order.domain.entity.Order;
import com.magambell.server.order.domain.entity.OrderGoods;
import com.magambell.server.order.domain.enums.OrderStatus;
import com.magambell.server.order.domain.repository.OrderRepository;
import com.magambell.server.payment.app.port.in.dto.CreatePaymentDTO;
import com.magambell.server.payment.app.port.out.response.StoreSalesSummaryDTO;
import com.magambell.server.payment.domain.entity.Payment;
import com.magambell.server.payment.domain.enums.PaymentCompletionType;
import com.magambell.server.payment.domain.enums.PaymentStatus;
import com.magambell.server.payment.infra.PortOnePaymentResponse;
import com.magambell.server.store.app.port.in.dto.RegisterStoreDTO;
import com.magambell.server.store.domain.entity.Store;
import com.magambell.server.store.domain.enums.Approved;
import com.magambell.server.store.domain.enums.Bank;
import com.magambell.server.store.domain.repository.StoreRepository;
import com.magambell.server.user.app.port.in.dto.UserSocialAccountDTO;
import com.magambell.server.user.domain.entity.User;
import com.magambell.server.user.domain.enums.UserRole;
import com.magambell.server.user.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@Transactional
@SpringBootTest
class PaymentSalesSummaryRepositoryTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private EntityManager entityManager;

    @DisplayName("매장 매출은 유효한 주문과 결제만 전체 및 이번 달 실적으로 집계한다")
    @Test
    void getStoreSalesSummaryAggregatesOnlyEligiblePayments() {
        // given
        LocalDateTime monthStartAt = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime calculatedAt = LocalDateTime.of(2026, 9, 19, 12, 0);
        Store store = createStore("target");
        Goods goods = createGoods(store, "target-goods");
        Goods otherStoreGoods = createGoods(createStore("other"), "other-goods");

        createPaidOrder(goods, 10_000, monthStartAt, OrderStatus.PAID);
        createPaidOrder(goods, 20_000, LocalDateTime.of(2026, 9, 10, 10, 0), OrderStatus.ACCEPTED);
        createPaidOrder(goods, 30_000, LocalDateTime.of(2026, 8, 20, 9, 0), OrderStatus.COMPLETED);
        createPaidOrder(otherStoreGoods, 40_000, LocalDateTime.of(2026, 9, 5, 10, 0), OrderStatus.COMPLETED);
        createPaidOrder(goods, 50_000, LocalDateTime.of(2026, 9, 11, 10, 0), OrderStatus.PAID)
                .cancel(PaymentCompletionType.REDIRECT);
        createPaidOrder(goods, 60_000, calculatedAt.plusSeconds(1), OrderStatus.COMPLETED);
        createPaidOrder(goods, 70_000, LocalDateTime.of(2026, 9, 12, 10, 0), OrderStatus.PAID)
                .getOrder().failed();
        flushAndClear();

        // when
        StoreSalesSummaryDTO result = paymentRepository.getStoreSalesSummary(
                store.getId(), monthStartAt, calculatedAt);

        // then
        assertThat(result.totalAmount()).isEqualTo(60_000L);
        assertThat(result.totalOrderCount()).isEqualTo(3L);
        assertThat(result.monthlyAmount()).isEqualTo(30_000L);
        assertThat(result.monthlyOrderCount()).isEqualTo(2L);
        assertThat(result.firstSoldAt()).isEqualTo(LocalDateTime.of(2026, 8, 20, 9, 0));
    }

    @DisplayName("매출이 없으면 합계와 건수는 0이고 최초 판매 일시는 없다")
    @Test
    void getStoreSalesSummaryReturnsZeroWhenThereAreNoSales() {
        // given
        Store store = createStore("empty");

        // when
        StoreSalesSummaryDTO result = paymentRepository.getStoreSalesSummary(
                store.getId(),
                LocalDateTime.of(2026, 9, 1, 0, 0),
                LocalDateTime.of(2026, 9, 19, 12, 0));

        // then
        assertThat(result.totalAmount()).isZero();
        assertThat(result.totalOrderCount()).isZero();
        assertThat(result.monthlyAmount()).isZero();
        assertThat(result.monthlyOrderCount()).isZero();
        assertThat(result.firstSoldAt()).isNull();
    }

    @DisplayName("한 주문에 같은 매장의 상품이 여러 개여도 결제는 한 번만 집계한다")
    @Test
    void getStoreSalesSummaryDoesNotDuplicatePaymentForMultipleOrderGoods() {
        // given
        LocalDateTime monthStartAt = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime calculatedAt = LocalDateTime.of(2026, 9, 19, 12, 0);
        Store store = createStore("multiple-goods");
        Goods firstGoods = createGoods(store, "first-goods");
        Goods secondGoods = createGoods(store, "second-goods");
        Payment payment = createPaidOrder(firstGoods, 25_000,
                LocalDateTime.of(2026, 9, 10, 10, 0), OrderStatus.COMPLETED);
        Order order = payment.getOrder();
        OrderGoods additionalOrderGoods = new CreateOrderDTO(
                order.getUser(), secondGoods, 1, secondGoods.getSalePrice(), order.getPickupTime(), null)
                .toOrderGoods();
        order.addOrderGoods(additionalOrderGoods);
        additionalOrderGoods.addGoods(secondGoods);
        flushAndClear();

        // when
        StoreSalesSummaryDTO result = paymentRepository.getStoreSalesSummary(
                store.getId(), monthStartAt, calculatedAt);

        // then
        assertThat(result.totalAmount()).isEqualTo(25_000L);
        assertThat(result.totalOrderCount()).isEqualTo(1L);
        assertThat(result.monthlyAmount()).isEqualTo(25_000L);
        assertThat(result.monthlyOrderCount()).isEqualTo(1L);
    }

    @DisplayName("판매 금액 합계는 Integer 범위를 넘어도 Long으로 집계한다")
    @Test
    void getStoreSalesSummaryUsesLongForAmountSum() {
        // given
        LocalDateTime monthStartAt = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime calculatedAt = LocalDateTime.of(2026, 9, 19, 12, 0);
        Store store = createStore("large-amount");
        Goods goods = createGoods(store, "large-amount-goods");
        createPaidOrder(goods, 2_000_000_000,
                LocalDateTime.of(2026, 9, 10, 10, 0), OrderStatus.COMPLETED);
        createPaidOrder(goods, 2_000_000_000,
                LocalDateTime.of(2026, 9, 11, 10, 0), OrderStatus.COMPLETED);
        flushAndClear();

        // when
        StoreSalesSummaryDTO result = paymentRepository.getStoreSalesSummary(
                store.getId(), monthStartAt, calculatedAt);

        // then
        assertThat(result.totalAmount()).isEqualTo(4_000_000_000L);
        assertThat(result.monthlyAmount()).isEqualTo(4_000_000_000L);
        assertThat(result.totalOrderCount()).isEqualTo(2L);
        assertThat(result.monthlyOrderCount()).isEqualTo(2L);
    }

    @DisplayName("결제가 취소되면 원 결제일의 전체 및 월간 매출에서 제외한다")
    @Test
    void getStoreSalesSummaryExcludesCancelledPaymentRetroactively() {
        // given
        LocalDateTime monthStartAt = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime calculatedAt = LocalDateTime.of(2026, 9, 19, 12, 0);
        Store store = createStore("cancelled");
        Goods goods = createGoods(store, "cancelled-goods");
        Payment payment = createPaidOrder(goods, 15_000,
                LocalDateTime.of(2026, 9, 5, 10, 0), OrderStatus.PAID);
        flushAndClear();

        // when
        StoreSalesSummaryDTO beforeCancel = paymentRepository.getStoreSalesSummary(
                store.getId(), monthStartAt, calculatedAt);
        Payment savedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        savedPayment.getOrder().cancelled(PaymentCompletionType.REDIRECT);
        flushAndClear();
        StoreSalesSummaryDTO afterCancel = paymentRepository.getStoreSalesSummary(
                store.getId(), monthStartAt, calculatedAt);

        // then
        assertThat(beforeCancel.totalAmount()).isEqualTo(15_000L);
        assertThat(beforeCancel.monthlyAmount()).isEqualTo(15_000L);
        assertThat(afterCancel.totalAmount()).isZero();
        assertThat(afterCancel.totalOrderCount()).isZero();
        assertThat(afterCancel.monthlyAmount()).isZero();
        assertThat(afterCancel.monthlyOrderCount()).isZero();
        assertThat(afterCancel.firstSoldAt()).isNull();
    }

    private Payment createPaidOrder(final Goods goods, final int amount, final LocalDateTime paidAt,
                                    final OrderStatus finalStatus) {
        User customer = createUser(UserRole.CUSTOMER, "customer");
        Order order = new CreateOrderDTO(
                customer,
                goods,
                1,
                amount,
                paidAt.plusHours(1),
                null
        ).toOrder();
        orderRepository.saveAndFlush(order);

        Payment payment = new CreatePaymentDTO(order, amount, PaymentStatus.READY).toPayment();
        payment.paid(paymentResponse(payment, amount, paidAt), PaymentCompletionType.REDIRECT);
        if (finalStatus == OrderStatus.ACCEPTED) {
            order.accepted();
        } else if (finalStatus == OrderStatus.COMPLETED) {
            order.accepted();
            order.completed();
        }
        return paymentRepository.save(payment);
    }

    private PortOnePaymentResponse paymentResponse(final Payment payment, final int amount,
                                                   final LocalDateTime paidAt) {
        return new PortOnePaymentResponse(
                payment.getMerchantUid(),
                "transaction-" + SEQUENCE.incrementAndGet(),
                "merchant",
                PaymentStatus.PAID,
                paidAt.atZone(SEOUL).toOffsetDateTime(),
                new PortOnePaymentResponse.Method("CARD", null, "TEST"),
                new PortOnePaymentResponse.Amount(amount)
        );
    }

    private Store createStore(final String name) {
        User owner = createUser(UserRole.OWNER, name + "-owner");
        Store store = new RegisterStoreDTO(
                name,
                "서울시",
                37.0,
                127.0,
                "대표",
                "01012345678",
                "business-" + SEQUENCE.incrementAndGet(),
                Bank.KB국민,
                "1234567890",
                List.of(),
                Approved.APPROVED,
                owner,
                null,
                null
        ).toEntity();
        owner.addStore(store);
        return storeRepository.save(store);
    }

    private Goods createGoods(final Store store, final String name) {
        Goods goods = new RegisterGoodsDTO(
                name,
                LocalDateTime.of(2026, 9, 19, 9, 0),
                LocalDateTime.of(2026, 9, 19, 18, 0),
                100,
                10_000,
                10,
                9_000,
                store,
                List.of(new GoodsImagesRegister(0, "key", "https://example.com/image.jpg", name))
        ).toGoods();
        store.addGoods(goods);
        return goodsRepository.save(goods);
    }

    private User createUser(final UserRole role, final String name) {
        int sequence = SEQUENCE.incrementAndGet();
        UserSocialAccountDTO dto = new UserSocialAccountDTO(
                name + sequence + "@test.com",
                name,
                name + sequence,
                "01012345678",
                ProviderType.KAKAO,
                "provider-" + sequence,
                role
        );
        return userRepository.save(dto.toUser());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
