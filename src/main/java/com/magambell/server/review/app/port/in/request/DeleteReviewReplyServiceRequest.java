package com.magambell.server.review.app.port.in.request;

public record DeleteReviewReplyServiceRequest(
        Long reviewId,
        Long userId
) {
}
