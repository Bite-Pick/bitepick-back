package com.magambell.server.review.app.port.out.response;

import java.time.LocalDateTime;

public record ReviewReplyDTO(
        Long replyId,
        String content,
        LocalDateTime createdAt
) {
    public String getReplyId() {
        return String.valueOf(replyId);
    }
}
