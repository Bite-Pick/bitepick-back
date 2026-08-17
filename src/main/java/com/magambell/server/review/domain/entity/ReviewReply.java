package com.magambell.server.review.domain.entity;

import com.magambell.server.common.BaseTimeEntity;
import com.magambell.server.review.domain.enums.ReviewReplyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_review_reply_review_id", columnNames = "review_id")
        }
)
@Entity
public class ReviewReply extends BaseTimeEntity {

    @Column(name = "review_reply_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(nullable = false, length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewReplyStatus replyStatus;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Builder(access = AccessLevel.PRIVATE)
    private ReviewReply(final String content, final ReviewReplyStatus replyStatus) {
        this.content = content;
        this.replyStatus = replyStatus;
    }

    public static ReviewReply create(final String content) {
        return ReviewReply.builder()
                .content(content)
                .replyStatus(ReviewReplyStatus.ACTIVE)
                .build();
    }

    public boolean isActive() {
        return this.replyStatus == ReviewReplyStatus.ACTIVE;
    }

    public void addReview(final Review review) {
        this.review = review;
    }

    public void restore(final String content) {
        this.content = content;
        this.replyStatus = ReviewReplyStatus.ACTIVE;
    }

    public void delete() {
        this.replyStatus = ReviewReplyStatus.DELETED;
    }
}
