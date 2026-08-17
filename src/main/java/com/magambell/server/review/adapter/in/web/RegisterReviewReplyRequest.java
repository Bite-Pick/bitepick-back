package com.magambell.server.review.adapter.in.web;

import com.magambell.server.review.app.port.in.request.RegisterReviewReplyServiceRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterReviewReplyRequest(
        @NotBlank(message = "답글을 작성해 주세요.")
        @Size(max = 500, message = "답글은 최대 500자까지 작성할 수 있습니다.")
        String content
) {

    public RegisterReviewReplyServiceRequest toServiceRequest(final Long reviewId, final Long userId) {
        return new RegisterReviewReplyServiceRequest(reviewId, userId, content);
    }
}
