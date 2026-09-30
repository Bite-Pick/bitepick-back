package com.magambell.server.review.app.port.in.request;

public record EditReviewReplyServiceRequest(
        Long reviewId,
        Long userId,
        String content
) {
}
