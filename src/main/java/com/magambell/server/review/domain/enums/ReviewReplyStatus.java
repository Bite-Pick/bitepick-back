package com.magambell.server.review.domain.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ReviewReplyStatus {
    ACTIVE("등록"),
    DELETED("삭제");

    private final String text;
}
