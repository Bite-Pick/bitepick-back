package com.magambell.server.notification.app.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.magambell.server.goods.domain.entity.Goods;
import com.magambell.server.notification.app.port.in.request.NotifyStoreOpenRequest;
import com.magambell.server.notification.app.port.out.NotificationCommandPort;
import com.magambell.server.notification.app.port.out.NotificationQueryPort;
import com.magambell.server.notification.app.port.out.dto.FcmTokenDTO;
import com.magambell.server.notification.infra.FirebaseNotificationSender;
import com.magambell.server.order.app.port.out.OrderQueryPort;
import com.magambell.server.order.domain.entity.Order;
import com.magambell.server.order.domain.entity.OrderGoods;
import com.magambell.server.review.app.port.out.ReviewQueryPort;
import com.magambell.server.store.app.port.out.StoreQueryPort;
import com.magambell.server.store.domain.entity.Store;
import com.magambell.server.user.app.port.out.UserQueryPort;
import com.magambell.server.user.domain.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationMessageTest {

    @Mock
    private NotificationCommandPort notificationCommandPort;

    @Mock
    private NotificationQueryPort notificationQueryPort;

    @Mock
    private FirebaseNotificationSender firebaseNotificationSender;

    @Mock
    private StoreQueryPort storeQueryPort;

    @Mock
    private UserQueryPort userQueryPort;

    @Mock
    private OrderQueryPort orderQueryPort;

    @Mock
    private ReviewQueryPort reviewQueryPort;

    @InjectMocks
    private NotificationService notificationService;

    @DisplayName("매장 오픈 알림의 닉네임과 가게명에는 괄호를 붙이지 않는다")
    @Test
    void notifyStoreOpenWithoutBrackets() throws FirebaseMessagingException {
        Store store = mock(Store.class);
        FcmTokenDTO token = new FcmTokenDTO(1L, "customer-token", 2L, "고객닉네임", "테스트 매장");
        when(store.getId()).thenReturn(3L);
        when(notificationQueryPort.findWithAllByStoreId(store)).thenReturn(List.of(token));

        notificationService.notifyStoreOpen(new NotifyStoreOpenRequest(store));

        verify(firebaseNotificationSender).send(
                "customer-token",
                "바이트픽",
                "고객닉네임님이 기다리던 테스트 매장의 바이트백 판매가 시작됐어요!",
                Map.of("type", "STORE_OPEN", "storeId", "3")
        );
    }

    @DisplayName("픽업 알림의 가게명에는 괄호를 붙이지 않는다")
    @Test
    void notifyPickupWithoutStoreNameBrackets() throws FirebaseMessagingException {
        LocalDateTime pickupTime = LocalDateTime.of(2026, 9, 17, 15, 30);
        Order order = mock(Order.class);
        OrderGoods orderGoods = mock(OrderGoods.class);
        Goods goods = mock(Goods.class);
        Store store = mock(Store.class);
        User customer = mock(User.class);
        User owner = mock(User.class);
        FcmTokenDTO customerToken = new FcmTokenDTO(1L, "customer-token", 10L, null, null);
        FcmTokenDTO ownerToken = new FcmTokenDTO(2L, "owner-token", 20L, null, null);

        when(customer.getId()).thenReturn(10L);
        when(owner.getId()).thenReturn(20L);
        when(order.getUser()).thenReturn(customer);
        when(order.getOrderStoreOwner()).thenReturn(Set.of(owner));
        when(order.getOrderGoodsList()).thenReturn(List.of(orderGoods));
        when(orderGoods.getGoods()).thenReturn(goods);
        when(goods.getStore()).thenReturn(store);
        when(store.getUser()).thenReturn(owner);
        when(store.getName()).thenReturn("테스트 매장");
        when(orderQueryPort.findOrdersToNotifyByPickupTime(pickupTime)).thenReturn(List.of(order));
        when(notificationQueryPort.findByUsers(Set.of(customer))).thenReturn(List.of(customerToken));
        when(notificationQueryPort.findByUsers(Set.of(owner))).thenReturn(List.of(ownerToken));

        notificationService.notifyPickup(pickupTime);

        verify(firebaseNotificationSender).send(
                "customer-token",
                "바이트픽",
                "테스트 매장에서 바이트백을 픽업해주세요!",
                Map.of("type", "ORDER")
        );
        verify(firebaseNotificationSender).send(
                "owner-token",
                "바이트픽",
                "테스트 매장의 픽업 가능 시간이 시작되었습니다.",
                Map.of("type", "ORDER")
        );
    }

    @DisplayName("주문 수락 알림의 픽업 시간에는 괄호를 붙이지 않는다")
    @Test
    void notifyApproveOrderWithoutPickupTimeBrackets() throws FirebaseMessagingException {
        User customer = mock(User.class);
        FcmTokenDTO token = new FcmTokenDTO(1L, "customer-token", 10L, null, null);
        LocalDateTime pickupTime = LocalDateTime.of(2026, 9, 17, 15, 30);
        when(notificationQueryPort.findWithAllByUserIdAndStoreIsNull(customer)).thenReturn(token);

        notificationService.notifyApproveOrder(customer, pickupTime);

        verify(firebaseNotificationSender).send(
                "customer-token",
                "바이트픽",
                "주문이 수락됐어요. 15:30에 바이트백을 픽업 해주세요!",
                Map.of("type", "ORDER")
        );
    }
}
