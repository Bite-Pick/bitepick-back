package com.magambell.server.review.app.port.in.request;

import com.magambell.server.review.domain.enums.ReviewStoreFilter;

public record ReviewStoreServiceRequest(
        Long userId,
        ReviewStoreFilter filter,
        Long cursor,
        Integer size
) {
}
