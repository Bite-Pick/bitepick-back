package com.magambell.server.notification.app.event;

import com.magambell.server.notification.app.port.in.NotificationUseCase;
import com.magambell.server.notification.app.port.in.request.NotifyReviewReplyRequest;
import com.magambell.server.review.app.event.ReviewReplyRegisteredEvent;
import com.magambell.server.store.app.port.out.StoreQueryPort;
import com.magambell.server.store.domain.entity.Store;
import com.magambell.server.user.app.port.out.UserQueryPort;
import com.magambell.server.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Component
public class ReviewReplyNotificationEventListener {

    private final UserQueryPort userQueryPort;
    private final StoreQueryPort storeQueryPort;
    private final NotificationUseCase notificationUseCase;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(final ReviewReplyRegisteredEvent event) {
        User reviewAuthor = userQueryPort.findById(event.reviewAuthorId());
        Store store = storeQueryPort.findById(event.storeId());

        notificationUseCase.notifyReviewReply(new NotifyReviewReplyRequest(reviewAuthor, store));
    }
}
