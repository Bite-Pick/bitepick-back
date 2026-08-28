package com.magambell.server.review.app.port.out.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record ReviewStoreItemDTO(
        Long reviewId,
        Integer rating,
        String description,
        List<String> imageUrls,
        String nickName,
        LocalDateTime orderedAt,
        LocalDateTime createdAt,
        String productName,
        ReviewReplyDTO reply
) {
    public ReviewStoreItemDTO {
        imageUrls = imageUrls == null ? List.of() : imageUrls.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    public String getReviewId() {
        return String.valueOf(reviewId);
    }
}
