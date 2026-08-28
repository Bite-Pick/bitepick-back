package com.magambell.server.review.app.port.out.response;

public record ReviewStoreSummaryDTO(
        Double averageRating,
        Long totalCount,
        Long noReplyCount
) {
}
