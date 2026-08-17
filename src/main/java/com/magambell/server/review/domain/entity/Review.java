package com.magambell.server.review.domain.entity;

import com.magambell.server.common.BaseTimeEntity;
import com.magambell.server.common.enums.ErrorCode;
import com.magambell.server.common.exception.DuplicateException;
import com.magambell.server.common.exception.InvalidRequestException;
import com.magambell.server.order.domain.entity.OrderGoods;
import com.magambell.server.review.app.port.in.dto.RegisterReviewDTO;
import com.magambell.server.review.domain.enums.ReviewStatus;
import com.magambell.server.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Review extends BaseTimeEntity {

    @Column(name = "review_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private Integer rating;

    private String description;

    @Enumerated(EnumType.STRING)
    private ReviewStatus reviewStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_goods_id")
    private OrderGoods orderGoods;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL)
    private List<ReviewImage> reviewImages = new ArrayList<>();

    @OneToOne(mappedBy = "review", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ReviewReply reviewReply;

    @Builder(access = AccessLevel.PRIVATE)
    private Review(final Integer rating, final String description, final ReviewStatus reviewStatus) {
        this.rating = rating;
        this.description = description;
        this.reviewStatus = reviewStatus;
    }

    public static Review create(final RegisterReviewDTO dto) {
        if (dto.rating() < 1 || dto.rating() > 3) {
            throw new InvalidRequestException(ErrorCode.INVALID_REVIEW_RATING);
        }

        Review review = Review.builder()
                .rating(dto.rating())
                .description(dto.description())
                .reviewStatus(ReviewStatus.ACTIVE)
                .build();

        review.addUser(dto.user());
        review.addOrderGoods(dto.orderGoods());

        return review;

    }

    private void addOrderGoods(final OrderGoods orderGoods) {
        this.orderGoods = orderGoods;
    }

    private void addUser(final User user) {
        this.user = user;
        user.addReview(this);
    }

    public void addReviewImage(final ReviewImage reviewImage) {
        this.reviewImages.add(reviewImage);
        reviewImage.addReview(this);
    }

    public ReviewReply addReviewReply(final String content) {
        if (this.reviewReply != null && this.reviewReply.isActive()) {
            throw new DuplicateException(ErrorCode.DUPLICATE_REVIEW_REPLY);
        }

        if (this.reviewReply != null) {
            this.reviewReply.restore(content);
            return this.reviewReply;
        }

        ReviewReply reply = ReviewReply.create(content);
        this.reviewReply = reply;
        reply.addReview(this);
        return reply;
    }

    public void deleteReviewReply() {
        if (this.reviewReply != null) {
            this.reviewReply.delete();
        }
    }

    public void delete() {
        this.reviewStatus = ReviewStatus.DELETED;
        deleteReviewReply();
    }
}
