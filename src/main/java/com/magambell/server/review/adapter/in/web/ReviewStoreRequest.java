package com.magambell.server.review.adapter.in.web;

import com.magambell.server.review.app.port.in.request.ReviewStoreServiceRequest;
import com.magambell.server.review.domain.enums.ReviewStoreFilter;
import jakarta.validation.constraints.Positive;

public record ReviewStoreRequest(
        ReviewStoreFilter filter,
        Long cursor,
        @Positive(message = "화면에 개수를 주세요.")
        Integer size
) {
    private static final int DEFAULT_SIZE = 20;

    public ReviewStoreServiceRequest toServiceRequest(final Long userId) {
        return new ReviewStoreServiceRequest(
                userId,
                filter == null ? ReviewStoreFilter.ALL : filter,
                cursor,
                size == null ? DEFAULT_SIZE : size
        );
    }
}
