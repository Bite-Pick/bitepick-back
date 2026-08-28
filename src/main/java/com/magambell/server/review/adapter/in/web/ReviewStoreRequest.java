package com.magambell.server.review.adapter.in.web;

import com.magambell.server.review.app.port.in.request.ReviewStoreServiceRequest;
import com.magambell.server.review.domain.enums.ReviewStoreFilter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

public record ReviewStoreRequest(
        ReviewStoreFilter filter,
        Long cursor,
        @Positive(message = "화면에 개수를 주세요.")
        @Max(value = MAX_SIZE, message = "페이지 크기가 최대값을 초과했습니다.")
        Integer size
) {
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    public ReviewStoreServiceRequest toServiceRequest(final Long userId) {
        return new ReviewStoreServiceRequest(
                userId,
                filter == null ? ReviewStoreFilter.ALL : filter,
                cursor,
                size == null ? DEFAULT_SIZE : size
        );
    }
}
