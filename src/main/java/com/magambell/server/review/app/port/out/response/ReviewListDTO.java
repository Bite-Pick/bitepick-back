package com.magambell.server.review.app.port.out.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record ReviewListDTO(
        Long reviewId,
        Integer rating,
        String description,
        LocalDateTime createdAt,
        List<String> imageUrls,
        String nickName,
        Long goodsId,
        Long storeId,
        String storeName,
        ReviewReplyDTO reply
) {
    public ReviewListDTO {
        imageUrls = imageUrls == null ? List.of() : imageUrls.stream()
                .filter(Objects::nonNull)
                .toList();
    }
    public String getReviewId() {
        return String.valueOf(reviewId);
    }

    public String getGoodsId() {
        return String.valueOf(goodsId);
    }

    public String getStoreId() {
        return String.valueOf(storeId);
    }
}
