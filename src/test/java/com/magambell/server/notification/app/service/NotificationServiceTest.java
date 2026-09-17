package com.magambell.server.notification.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.magambell.server.auth.domain.ProviderType;
import com.magambell.server.goods.domain.repository.GoodsRepository;
import com.magambell.server.notification.app.port.in.request.SaveFcmTokenServiceRequest;
import com.magambell.server.notification.app.port.in.request.DeleteStoreOpenFcmTokenServiceRequest;
import com.magambell.server.notification.app.port.in.request.NotifyReviewReplyRequest;
import com.magambell.server.notification.app.port.in.request.SaveStoreOpenFcmTokenServiceRequest;
import com.magambell.server.notification.adapter.in.web.CheckStoreOpenServiceRequest;
import com.magambell.server.notification.domain.entity.FcmToken;
import com.magambell.server.notification.domain.repository.FcmTokenRepository;
import com.magambell.server.notification.infra.FirebaseNotificationSender;
import com.magambell.server.review.app.port.out.ReviewQueryPort;
import com.magambell.server.review.app.port.out.response.OwnerReviewCountDTO;
import com.magambell.server.stock.domain.repository.StockHistoryRepository;
import com.magambell.server.stock.domain.repository.StockRepository;
import com.magambell.server.store.app.port.in.dto.RegisterStoreDTO;
import com.magambell.server.store.domain.enums.Approved;
import com.magambell.server.store.domain.enums.Bank;
import com.magambell.server.store.domain.entity.Store;
import com.magambell.server.store.domain.repository.StoreRepository;
import com.magambell.server.user.app.port.in.dto.UserSocialAccountDTO;
import com.magambell.server.user.domain.enums.UserRole;
import com.magambell.server.user.domain.entity.User;
import com.magambell.server.user.domain.repository.UserRepository;
import com.magambell.server.user.domain.repository.UserSocialAccountRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private FcmTokenRepository fcmTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSocialAccountRepository userSocialAccountRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    @MockBean
    private FirebaseNotificationSender firebaseNotificationSender;

    @MockBean
    private ReviewQueryPort reviewQueryPort;

    private User user;
    private Store store;

    @BeforeEach
    void setUp() {
        UserSocialAccountDTO userSocialAccountDTO = new UserSocialAccountDTO("test@test.com", "테스트이름", "닉네임",
                "01012341234",
                ProviderType.KAKAO,
                "testId", UserRole.OWNER);
        user = userSocialAccountDTO.toUser();
        user.addUserSocialAccount(userSocialAccountDTO.toUserSocialAccount());

        RegisterStoreDTO registerStoreDTO = new RegisterStoreDTO(
                "테스트 매장",
                "서울 강서구 테스트 211",
                1238.123213,
                5457.123213,
                "대표이름",
                "01012345678",
                "123491923",
                Bank.KB국민,
                "102391485",
                List.of(),
                Approved.APPROVED,
                user,
                null,
                "주차장"
        );
        store = registerStoreDTO.toEntity();
        user.addStore(store);
        user = userRepository.save(user);
    }

    @AfterEach
    void tearDown() {
        fcmTokenRepository.deleteAllInBatch();
        stockHistoryRepository.deleteAllInBatch();
        stockRepository.deleteAllInBatch();
        goodsRepository.deleteAllInBatch();
        storeRepository.deleteAllInBatch();
        userSocialAccountRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @DisplayName("매장 오픈 FCM 토큰을 저장한다.")
    @Test
    void saveStoreOpenToken() {
        // given
        SaveStoreOpenFcmTokenServiceRequest request = new SaveStoreOpenFcmTokenServiceRequest(store.getId(),
                "testToken", user.getId());

        // when
        notificationService.saveStoreOpenToken(request);

        // then
        List<FcmToken> fcmTokenList = fcmTokenRepository.findAll();
        assertThat(fcmTokenList.size()).isEqualTo(1);
        assertThat(fcmTokenList.get(0).getToken()).isEqualTo("testToken");
    }

    @DisplayName("FCM용 토큰을 저장한다.")
    @Test
    void saveToken() {
        // given
        SaveFcmTokenServiceRequest request = new SaveFcmTokenServiceRequest("testToken", user.getId());

        // when
        notificationService.saveToken(request);

        // then
        List<FcmToken> fcmTokenList = fcmTokenRepository.findAll();
        assertThat(fcmTokenList.size()).isEqualTo(1);
        assertThat(fcmTokenList.get(0).getToken()).isEqualTo("testToken");
    }

    @DisplayName("구독 여부가 false여도 동일 디바이스 토큰의 매장 구독은 취소된다.")
    @Test
    void deleteStoreOpenTokenWhenSubscribedFalseButSameDeviceTokenExists() {
        // given
        String sharedToken = "sharedDeviceToken";
        notificationService.saveToken(new SaveFcmTokenServiceRequest(sharedToken, user.getId()));

        User anotherUser = createAndSaveUser("other@test.com", "otherSocialId", "다른닉네임", "01056781234");
        notificationService.saveStoreOpenToken(
            new SaveStoreOpenFcmTokenServiceRequest(store.getId(), sharedToken, anotherUser.getId()));

        boolean subscribed = notificationService.checkUserStoreOpen(
            new CheckStoreOpenServiceRequest(store.getId(), user.getId()));
        assertThat(subscribed).isFalse();

        // when
        notificationService.deleteStoreOpenToken(
            new DeleteStoreOpenFcmTokenServiceRequest(store.getId(), user.getId()));

        // then
        long remainStoreOpenSubscriptions = fcmTokenRepository.findAll().stream()
            .filter(token -> token.getStore() != null && token.getStore().getId().equals(store.getId()))
            .count();

        assertThat(remainStoreOpenSubscriptions).isZero();
    }

    @DisplayName("매장 오픈 알림 신청자 수를 집계한다.")
    @Test
    void getStoreOpenSubscriberCount() {
        // given
        notificationService.saveStoreOpenToken(
                new SaveStoreOpenFcmTokenServiceRequest(store.getId(), "token-1", user.getId()));

        User anotherUser = createAndSaveUser("count@test.com", "countSocialId", "카운트닉", "01011112222");
        notificationService.saveStoreOpenToken(
                new SaveStoreOpenFcmTokenServiceRequest(store.getId(), "token-2", anotherUser.getId()));

        // when
        long subscriberCount = notificationService.getStoreOpenSubscriberCount(store.getId());

        // then
        assertThat(subscriberCount).isEqualTo(2L);
    }

    @DisplayName("매장 오픈 알림 신청자 수는 중복 token row가 있어도 user 기준으로 집계한다.")
    @Test
    void getStoreOpenSubscriberCountDistinctUsers() {
        // given
        notificationService.saveStoreOpenToken(
                new SaveStoreOpenFcmTokenServiceRequest(store.getId(), "token-1", user.getId()));
        fcmTokenRepository.save(FcmToken.create("token-duplicate", user, store));

        // when
        long subscriberCount = notificationService.getStoreOpenSubscriberCount(store.getId());

        // then
        assertThat(subscriberCount).isEqualTo(1L);
    }

    @DisplayName("매장 오픈 알림 신청자 수는 ACTIVE 여부와 관계없이 user 기준으로 집계한다.")
    @Test
    void getStoreOpenSubscriberCountIncludesWithdrawnUser() {
        // given
        notificationService.saveStoreOpenToken(
                new SaveStoreOpenFcmTokenServiceRequest(store.getId(), "token-1", user.getId()));

        User withdrawnUser = createAndSaveUser("withdrawn@test.com", "withdrawnSocialId", "탈퇴닉", "01033334444");
        notificationService.saveStoreOpenToken(
                new SaveStoreOpenFcmTokenServiceRequest(store.getId(), "token-2", withdrawnUser.getId()));

        withdrawnUser.withdraw();
        userRepository.save(withdrawnUser);

        // when
        long subscriberCount = notificationService.getStoreOpenSubscriberCount(store.getId());

        // then
        assertThat(subscriberCount).isEqualTo(2L);
    }

    @DisplayName("리뷰 작성자에게 확정된 문구로 답글 알림을 전송한다.")
    @Test
    void notifyReviewReply() throws FirebaseMessagingException {
        // given
        User customer = createAndSaveCustomer();
        notificationService.saveToken(new SaveFcmTokenServiceRequest("customer-token", customer.getId()));

        // when
        notificationService.notifyReviewReply(new NotifyReviewReplyRequest(customer, store));

        // then
        verify(firebaseNotificationSender).send(
                "customer-token",
                "💬답글이 달렸어요",
                "테스트 매장에서 회원님의 리뷰에 답글을 남겼어요. 확인해보세요!",
                Map.of()
        );
    }

    @DisplayName("리뷰 작성자의 FCM 토큰이 없으면 답글 알림을 전송하지 않는다.")
    @Test
    void notifyReviewReplyWithoutToken() {
        // given
        User customer = createAndSaveCustomer();

        // when
        notificationService.notifyReviewReply(new NotifyReviewReplyRequest(customer, store));

        // then
        verifyNoInteractions(firebaseNotificationSender);
    }

    @DisplayName("리뷰 답글 알림 전송에 실패하면 기존 정책에 따라 FCM 토큰을 삭제한다.")
    @Test
    void notifyReviewReplyRemovesTokenWhenFcmFails() throws FirebaseMessagingException {
        // given
        User customer = createAndSaveCustomer();
        notificationService.saveToken(new SaveFcmTokenServiceRequest("invalid-token", customer.getId()));
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        doThrow(exception).when(firebaseNotificationSender).send(
                "invalid-token",
                "💬답글이 달렸어요",
                "테스트 매장에서 회원님의 리뷰에 답글을 남겼어요. 확인해보세요!",
                Map.of()
        );

        // when
        notificationService.notifyReviewReply(new NotifyReviewReplyRequest(customer, store));

        // then
        assertThat(fcmTokenRepository.findByUserId(customer.getId())).isEmpty();
    }

    @DisplayName("KST 기준 전날 리뷰가 없으면 사장님 묶음 알림을 전송하지 않는다.")
    @Test
    void notifyDailyOwnerReviewSummaryWithoutReviews() {
        // given
        ZoneId asiaSeoul = ZoneId.of("Asia/Seoul");
        LocalDate beforeExecution = LocalDate.now(asiaSeoul);
        when(reviewQueryPort.getOwnerReviewCounts(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // when
        notificationService.notifyDailyOwnerReviewSummary();

        // then
        LocalDate afterExecution = LocalDate.now(asiaSeoul);
        ArgumentCaptor<LocalDateTime> startAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(reviewQueryPort).getOwnerReviewCounts(startAtCaptor.capture(), endAtCaptor.capture());

        LocalDateTime startAt = startAtCaptor.getValue();
        LocalDateTime endAt = endAtCaptor.getValue();
        assertThat(startAt).isEqualTo(endAt.minusDays(1));
        assertThat(startAt.toLocalTime()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(endAt.toLocalTime()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(endAt.toLocalDate()).isIn(beforeExecution, afterExecution);
        verifyNoInteractions(firebaseNotificationSender);
    }

    @DisplayName("사장님별 리뷰 개수를 하나의 묶음 알림으로 전송한다.")
    @Test
    void notifyDailyOwnerReviewSummary() throws FirebaseMessagingException {
        // given
        LocalDate today = LocalDate.of(2026, 9, 17);
        User otherOwner = createAndSaveUser(
                "summary-owner@test.com", "summaryOwnerSocialId", "다른사장님", "01044445555");
        notificationService.saveToken(new SaveFcmTokenServiceRequest("owner-token", user.getId()));
        notificationService.saveToken(new SaveFcmTokenServiceRequest("other-owner-token", otherOwner.getId()));
        when(reviewQueryPort.getOwnerReviewCounts(today.minusDays(1).atStartOfDay(), today.atStartOfDay()))
                .thenReturn(List.of(
                        new OwnerReviewCountDTO(user.getId(), store.getId(), 1L),
                        new OwnerReviewCountDTO(user.getId(), 998L, 2L),
                        new OwnerReviewCountDTO(otherOwner.getId(), 999L, 2L)
                ));

        // when
        notificationService.notifyDailyOwnerReviewSummary(today);

        // then
        verify(firebaseNotificationSender).send(
                "owner-token",
                "🔔새 리뷰가 도착했어요",
                "리뷰 3개가 사장님을 기다리고 있어요! 답글로 마음을 전해보세요🍞",
                Map.of()
        );
        verify(firebaseNotificationSender).send(
                "other-owner-token",
                "🔔새 리뷰가 도착했어요",
                "리뷰 2개가 사장님을 기다리고 있어요! 답글로 마음을 전해보세요🍞",
                Map.of()
        );
    }

    @DisplayName("FCM 토큰이 없는 사장님에게는 리뷰 묶음 알림을 전송하지 않는다.")
    @Test
    void notifyDailyOwnerReviewSummaryWithoutToken() {
        // given
        LocalDate today = LocalDate.of(2026, 9, 17);
        when(reviewQueryPort.getOwnerReviewCounts(today.minusDays(1).atStartOfDay(), today.atStartOfDay()))
                .thenReturn(List.of(new OwnerReviewCountDTO(user.getId(), store.getId(), 1L)));

        // when
        notificationService.notifyDailyOwnerReviewSummary(today);

        // then
        verifyNoInteractions(firebaseNotificationSender);
    }

    @DisplayName("한 사장님의 FCM 발송 실패가 다른 사장님의 묶음 알림을 막지 않는다.")
    @Test
    void notifyDailyOwnerReviewSummaryContinuesAfterFcmFailure() throws FirebaseMessagingException {
        // given
        LocalDate today = LocalDate.of(2026, 9, 17);
        User otherOwner = createAndSaveUser(
                "failure-owner@test.com", "failureOwnerSocialId", "실패테스트사장님", "01055556666");
        notificationService.saveToken(new SaveFcmTokenServiceRequest("failed-owner-token", user.getId()));
        notificationService.saveToken(new SaveFcmTokenServiceRequest("success-owner-token", otherOwner.getId()));
        when(reviewQueryPort.getOwnerReviewCounts(today.minusDays(1).atStartOfDay(), today.atStartOfDay()))
                .thenReturn(List.of(
                        new OwnerReviewCountDTO(user.getId(), store.getId(), 1L),
                        new OwnerReviewCountDTO(otherOwner.getId(), 999L, 2L)
                ));
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        doThrow(exception).when(firebaseNotificationSender).send(
                "failed-owner-token",
                "🔔새 리뷰가 도착했어요",
                "리뷰 1개가 사장님을 기다리고 있어요! 답글로 마음을 전해보세요🍞",
                Map.of()
        );

        // when
        notificationService.notifyDailyOwnerReviewSummary(today);

        // then
        verify(firebaseNotificationSender).send(
                "success-owner-token",
                "🔔새 리뷰가 도착했어요",
                "리뷰 2개가 사장님을 기다리고 있어요! 답글로 마음을 전해보세요🍞",
                Map.of()
        );
        assertThat(fcmTokenRepository.findByUserId(user.getId())).isEmpty();
        assertThat(fcmTokenRepository.findByUserId(otherOwner.getId())).hasSize(1);
    }

    private User createAndSaveUser(final String email, final String socialId, final String nickName,
            final String phoneNumber) {
        UserSocialAccountDTO userSocialAccountDTO = new UserSocialAccountDTO(
            email,
            "테스트이름",
            nickName,
            phoneNumber,
            ProviderType.KAKAO,
            socialId,
            UserRole.OWNER);

        User anotherUser = userSocialAccountDTO.toUser();
        anotherUser.addUserSocialAccount(userSocialAccountDTO.toUserSocialAccount());
        return userRepository.save(anotherUser);
    }

    private User createAndSaveCustomer() {
        UserSocialAccountDTO customerAccountDTO = new UserSocialAccountDTO(
                "customer@test.com",
                "고객",
                "고객닉네임",
                "01022223333",
                ProviderType.KAKAO,
                "customerSocialId",
                UserRole.CUSTOMER
        );
        User customer = customerAccountDTO.toUser();
        customer.addUserSocialAccount(customerAccountDTO.toUserSocialAccount());
        return userRepository.save(customer);
    }
}
