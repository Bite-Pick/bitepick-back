package com.magambell.server.review.app.port.in.request;

public record RegisterReviewReplyServiceRequest(
        Long reviewId,
        Long userId,
        String content
) {
}
