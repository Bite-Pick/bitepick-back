package com.magambell.server.notification.app.port.in.request;

import com.magambell.server.store.domain.entity.Store;
import com.magambell.server.user.domain.entity.User;

public record NotifyReviewReplyRequest(
        User reviewAuthor,
        Store store
) {
}
