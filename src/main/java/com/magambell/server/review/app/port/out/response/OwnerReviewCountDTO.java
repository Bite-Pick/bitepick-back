package com.magambell.server.review.app.port.out.response;

public record OwnerReviewCountDTO(
        Long ownerId,
        Long storeId,
        Long reviewCount
) {
}
