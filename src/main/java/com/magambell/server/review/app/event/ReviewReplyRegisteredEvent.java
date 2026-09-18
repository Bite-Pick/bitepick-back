package com.magambell.server.review.app.event;

public record ReviewReplyRegisteredEvent(
        Long reviewId,
        Long reviewAuthorId,
        Long storeId
) {
}
