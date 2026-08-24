package com.magambell.server.review.domain.repository;

import com.magambell.server.order.domain.enums.OrderStatus;
import com.magambell.server.review.app.port.in.request.ReviewListServiceRequest;
import com.magambell.server.review.app.port.in.request.ReviewRatingAllServiceRequest;
import com.magambell.server.review.app.port.in.request.ReviewStoreServiceRequest;
import com.magambell.server.review.app.port.out.response.ReviewListDTO;
import com.magambell.server.review.app.port.out.response.ReviewRatingSummaryDTO;
import com.magambell.server.review.app.port.out.response.ReviewReplyDTO;
import com.magambell.server.review.app.port.out.response.ReviewStoreItemDTO;
import com.magambell.server.review.app.port.out.response.ReviewStoreSummaryDTO;
import com.magambell.server.review.domain.enums.ReviewReplyStatus;
import com.magambell.server.review.domain.enums.ReviewStatus;
import com.magambell.server.review.domain.enums.ReviewStoreFilter;
import com.magambell.server.user.domain.enums.UserStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.magambell.server.goods.domain.entity.QGoods.goods;
import static com.magambell.server.order.domain.entity.QOrder.order;
import static com.magambell.server.order.domain.entity.QOrderGoods.orderGoods;
import static com.magambell.server.review.domain.entity.QReview.review;
import static com.magambell.server.review.domain.entity.QReviewImage.reviewImage;
import static com.magambell.server.review.domain.entity.QReviewReply.reviewReply;
import static com.magambell.server.review.domain.entity.QReviewReport.reviewReport;
import static com.magambell.server.store.domain.entity.QStore.store;
import static com.magambell.server.user.domain.entity.QUser.user;

@RequiredArgsConstructor
public class ReviewRepositoryImpl implements ReviewRepositoryCustom {

    private final JPAQueryFactory queryFactory;


    @Override
    public List<ReviewListDTO> getReviewList(final ReviewListServiceRequest request, final Pageable pageable) {
        BooleanBuilder conditions = new BooleanBuilder();
        conditions.and(goods.id.eq(request.goodsId()));
        conditions.and(user.userStatus.eq(UserStatus.ACTIVE));
        conditions.and(order.orderStatus.eq(OrderStatus.COMPLETED));
        conditions.and(review.reviewStatus.eq(ReviewStatus.ACTIVE));
        if (request.imageCheck()) {
            conditions.and(reviewImage.isNotNull());
        }

        return getReviewListDTOS(pageable, conditions, request.userId());
    }

    @Override
    public ReviewRatingSummaryDTO getReviewRatingAll(final ReviewRatingAllServiceRequest request) {
        BooleanBuilder conditions = new BooleanBuilder();
        conditions.and(goods.id.eq(request.goodsId()));
        conditions.and(user.userStatus.eq(UserStatus.ACTIVE));
        conditions.and(order.orderStatus.eq(OrderStatus.COMPLETED));
        conditions.and(review.reviewStatus.eq(ReviewStatus.ACTIVE));
        if (request.imageCheck()) {
            conditions.and(reviewImage.isNotNull());
        }

        NumberPath<Long> ratingCount = Expressions.numberPath(Long.class, "ratingCount");

        List<Tuple> results = queryFactory
                .select(review.rating, review.id.countDistinct().as(ratingCount))
                .from(review)
                .leftJoin(reviewImage).on(reviewImage.review.id.eq(review.id))
                .innerJoin(orderGoods).on(orderGoods.id.eq(review.orderGoods.id))
                .innerJoin(order).on(order.id.eq(orderGoods.order.id))
                .innerJoin(goods).on(goods.id.eq(orderGoods.goods.id))
                .innerJoin(store).on(store.id.eq(goods.store.id))
                .innerJoin(user).on(user.id.eq(review.user.id))
                .where(conditions)
                .groupBy(review.rating)
                .fetch();

        long totalCount = 0;
        long ratingSum = 0;
        long rating1 = 0, rating2 = 0, rating3 = 0;

        for (Tuple tuple : results) {
            Integer rating = tuple.get(review.rating);
            Long count = tuple.get(ratingCount);

            if (rating != null && count != null) {
                totalCount += count;
                ratingSum += rating * count;

                switch (rating) {
                    case 1 -> rating1 = count;
                    case 2 -> rating2 = count;
                    case 3 -> rating3 = count;
                    default -> { /* 3점 평점 시스템: 1~3 외 값은 무시 */ }
                }
            }
        }

        double averageRating = totalCount > 0 ? Math.round(((double) ratingSum / totalCount) * 10.0) / 10.0 : 0.0;

        return new ReviewRatingSummaryDTO(
                averageRating,
                totalCount,
                rating1,
                rating2,
                rating3
        );
    }

    @Override
    public List<ReviewListDTO> getReviewListByUser(final Long userId, final Pageable pageable) {
        BooleanBuilder conditions = new BooleanBuilder();
        conditions.and(review.user.id.eq(userId));
        conditions.and(review.reviewStatus.eq(ReviewStatus.ACTIVE));
        return getReviewListDTOS(pageable, conditions, null);
    }

    // 실제 리뷰 목록 items 조회
    @Override
    public List<ReviewStoreItemDTO> getStoreReviewList(final ReviewStoreServiceRequest request, final Long storeId) {
        BooleanBuilder conditions = storeReviewBaseConditions(storeId);
        // 조건 추가: cursor가 있는 경우 -> review.review_id < ?
        if (request.cursor() != null) {
            conditions.and(review.id.lt(request.cursor()));
        }
        // 조건 추가: filter가 NO_REPLY일 경우 -> review_reply.review_reply_id IS NULL
        if (request.filter() == ReviewStoreFilter.NO_REPLY) {
            conditions.and(reviewReply.id.isNull());
        }

        // 조회 대상 reviewId만 먼저 가져오는 1차 쿼리
        List<Long> reviewIds = queryFactory
                .select(review.id)
                .from(review)
                .innerJoin(orderGoods).on(orderGoods.id.eq(review.orderGoods.id))
                .innerJoin(order).on(order.id.eq(orderGoods.order.id))
                .innerJoin(goods).on(goods.id.eq(orderGoods.goods.id))
                .innerJoin(store).on(store.id.eq(goods.store.id))
                .innerJoin(user).on(user.id.eq(review.user.id))
                .leftJoin(reviewReply).on(reviewReply.review.id.eq(review.id)
                        .and(reviewReply.replyStatus.eq(ReviewReplyStatus.ACTIVE)))
                .where(conditions)
                .orderBy(review.id.desc())
                .limit(request.size() + 1L)
                .fetch();

        if (reviewIds.isEmpty()) {
            return List.of();
        }

        // review_id 목록으로 이미지/답글/상품/결제 시각 등을 다시 조회하는 2차 쿼리
        List<Tuple> rows = queryFactory
                .select(
                        review.id,
                        review.rating,
                        review.description,
                        review.createdAt,
                        reviewImage.name,
                        user.nickName,
                        order.createdAt,
                        goods.name,
                        reviewReply.id,
                        reviewReply.content,
                        reviewReply.createdAt
                )
                .from(review)
                .leftJoin(reviewImage).on(reviewImage.review.id.eq(review.id))
                .innerJoin(orderGoods).on(orderGoods.id.eq(review.orderGoods.id))
                .innerJoin(order).on(order.id.eq(orderGoods.order.id))
                .innerJoin(goods).on(goods.id.eq(orderGoods.goods.id))
                .innerJoin(store).on(store.id.eq(goods.store.id))
                .innerJoin(user).on(user.id.eq(review.user.id))
                .leftJoin(reviewReply).on(reviewReply.review.id.eq(review.id)
                        .and(reviewReply.replyStatus.eq(ReviewReplyStatus.ACTIVE)))
                .where(review.id.in(reviewIds))
                .orderBy(review.id.desc(), reviewImage.order.asc())
                .fetch();

        Map<Long, ReviewStoreItemBuilder> itemByReviewId = new LinkedHashMap<>();
        rows.forEach(row -> {
            Long reviewId = row.get(review.id);
            ReviewStoreItemBuilder builder = itemByReviewId.computeIfAbsent(reviewId, id ->
                    new ReviewStoreItemBuilder(
                            id,
                            row.get(review.rating),
                            row.get(review.description),
                            row.get(user.nickName),
                            row.get(order.createdAt),
                            row.get(review.createdAt),
                            row.get(goods.name),
                            createReply(row)
                    ));

            String imageUrl = row.get(reviewImage.name);
            if (imageUrl != null) {
                builder.imageUrls().add(imageUrl);
            }
        });

        return itemByReviewId.values().stream()
                .map(ReviewStoreItemBuilder::toDto)
                .toList();
    }

    // summary 구성에 필요한 집계 데이터를 조회
    @Override
    public ReviewStoreSummaryDTO getStoreReviewSummary(final Long storeId) {
        BooleanBuilder conditions = storeReviewBaseConditions(storeId);

        // 전체 리뷰 수 조회하는 1차 쿼리
        Long totalCount = queryFactory
                .select(review.id.countDistinct())
                .from(review)
                .innerJoin(orderGoods).on(orderGoods.id.eq(review.orderGoods.id))
                .innerJoin(order).on(order.id.eq(orderGoods.order.id))
                .innerJoin(goods).on(goods.id.eq(orderGoods.goods.id))
                .innerJoin(store).on(store.id.eq(goods.store.id))
                .innerJoin(user).on(user.id.eq(review.user.id))
                .where(conditions)
                .fetchOne();

        // 평균 평점을 조회하는 2차 쿼리
        Double averageRating = queryFactory
                .select(review.rating.avg())
                .from(review)
                .innerJoin(orderGoods).on(orderGoods.id.eq(review.orderGoods.id))
                .innerJoin(order).on(order.id.eq(orderGoods.order.id))
                .innerJoin(goods).on(goods.id.eq(orderGoods.goods.id))
                .innerJoin(store).on(store.id.eq(goods.store.id))
                .innerJoin(user).on(user.id.eq(review.user.id))
                .where(conditions)
                .fetchOne();

        // 미답변 리뷰 수를 조회하는 3차 쿼리
        Long noReplyCount = queryFactory
                .select(review.id.countDistinct())
                .from(review)
                .innerJoin(orderGoods).on(orderGoods.id.eq(review.orderGoods.id))
                .innerJoin(order).on(order.id.eq(orderGoods.order.id))
                .innerJoin(goods).on(goods.id.eq(orderGoods.goods.id))
                .innerJoin(store).on(store.id.eq(goods.store.id))
                .innerJoin(user).on(user.id.eq(review.user.id))
                .leftJoin(reviewReply).on(reviewReply.review.id.eq(review.id)
                        .and(reviewReply.replyStatus.eq(ReviewReplyStatus.ACTIVE)))
                .where(conditions.and(reviewReply.id.isNull()))
                .fetchOne();

        double roundedAverageRating = averageRating == null ? 0.0 : Math.round(averageRating * 10.0) / 10.0;
        return new ReviewStoreSummaryDTO(
                roundedAverageRating,
                totalCount == null ? 0L : totalCount,
                noReplyCount == null ? 0L : noReplyCount
        );
    }

    private List<ReviewListDTO> getReviewListDTOS(final Pageable pageable, final BooleanBuilder conditions, final Long userId) {
        // userId가 존재한다면, 해당 유저가 신고한 reviewId 목록 제외
        if (userId != null) {
            List<Long> reportedReviewIds = queryFactory
                    .select(reviewReport.review.id)
                    .from(reviewReport)
                    .where(reviewReport.user.id.eq(userId))
                    .fetch();

            if (!reportedReviewIds.isEmpty()) {
                conditions.and(review.id.notIn(reportedReviewIds));
            }
        }

        List<Long> reviewIds = queryFactory
                .selectDistinct(review.id)
                .from(review)
                .leftJoin(reviewImage).on(reviewImage.review.id.eq(review.id))
                .innerJoin(orderGoods).on(orderGoods.id.eq(review.orderGoods.id))
                .innerJoin(order).on(order.id.eq(orderGoods.order.id))
                .innerJoin(goods).on(goods.id.eq(orderGoods.goods.id))
                .innerJoin(store).on(store.id.eq(goods.store.id))
                .innerJoin(user).on(user.id.eq(review.user.id))
                .where(conditions)
                .orderBy(review.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        if (reviewIds.isEmpty()) {
            return List.of();
        }

        List<Tuple> rows = queryFactory
                .select(
                        review.id,
                        review.rating,
                        review.description,
                        review.createdAt,
                        reviewImage.name,
                        user.nickName,
                        goods.id,
                        store.id,
                        store.name,
                        reviewReply.id,
                        reviewReply.content,
                        reviewReply.createdAt
                )
                .from(review)
                .leftJoin(reviewImage).on(reviewImage.review.id.eq(review.id))
                .innerJoin(orderGoods).on(orderGoods.id.eq(review.orderGoods.id))
                .innerJoin(order).on(order.id.eq(orderGoods.order.id))
                .innerJoin(goods).on(goods.id.eq(orderGoods.goods.id))
                .innerJoin(store).on(store.id.eq(goods.store.id))
                .innerJoin(user).on(user.id.eq(review.user.id))
                .leftJoin(reviewReply).on(reviewReply.review.id.eq(review.id)
                        .and(reviewReply.replyStatus.eq(ReviewReplyStatus.ACTIVE)))
                .where(review.id.in(reviewIds))
                .orderBy(review.createdAt.desc(), reviewImage.order.asc())
                .fetch();

        Map<Long, ReviewListItemBuilder> itemByReviewId = new LinkedHashMap<>();
        rows.forEach(row -> {
            Long reviewId = row.get(review.id);
            ReviewListItemBuilder builder = itemByReviewId.computeIfAbsent(reviewId, id ->
                    new ReviewListItemBuilder(
                            id,
                            row.get(review.rating),
                            row.get(review.description),
                            row.get(review.createdAt),
                            row.get(user.nickName),
                            row.get(goods.id),
                            row.get(store.id),
                            row.get(store.name),
                            createReply(row)
                    ));

            String imageUrl = row.get(reviewImage.name);
            if (imageUrl != null) {
                builder.imageUrls().add(imageUrl);
            }
        });

        return itemByReviewId.values().stream()
                .map(ReviewListItemBuilder::toDto)
                .toList();
    }

    /* 리뷰 목록 items 조회 공통 조건
        - store.id = storeId
        - user.userStatus = ACTIVE
        - order.orderStatus = COMPLETED
        - review.reviewStatus = ACTIVE
     */
    private BooleanBuilder storeReviewBaseConditions(final Long storeId) {
        BooleanBuilder conditions = new BooleanBuilder();
        conditions.and(store.id.eq(storeId));
        conditions.and(user.userStatus.eq(UserStatus.ACTIVE));
        conditions.and(order.orderStatus.eq(OrderStatus.COMPLETED));
        conditions.and(review.reviewStatus.eq(ReviewStatus.ACTIVE));
        return conditions;
    }

    private ReviewReplyDTO createReply(final Tuple row) {
        Long replyId = row.get(reviewReply.id);
        if (replyId == null) {
            return null;
        }
        return new ReviewReplyDTO(replyId, row.get(reviewReply.content), row.get(reviewReply.createdAt));
    }

    private record ReviewListItemBuilder(
            Long reviewId,
            Integer rating,
            String description,
            LocalDateTime createdAt,
            String nickName,
            Long goodsId,
            Long storeId,
            String storeName,
            ReviewReplyDTO reply,
            List<String> imageUrls
    ) {
        private ReviewListItemBuilder(
                final Long reviewId,
                final Integer rating,
                final String description,
                final LocalDateTime createdAt,
                final String nickName,
                final Long goodsId,
                final Long storeId,
                final String storeName,
                final ReviewReplyDTO reply
        ) {
            this(reviewId, rating, description, createdAt, nickName, goodsId, storeId, storeName, reply,
                    new ArrayList<>());
        }

        private ReviewListDTO toDto() {
            return new ReviewListDTO(
                    reviewId,
                    rating,
                    description,
                    createdAt,
                    imageUrls,
                    nickName,
                    goodsId,
                    storeId,
                    storeName,
                    reply
            );
        }
    }

    private record ReviewStoreItemBuilder(
            Long reviewId,
            Integer rating,
            String description,
            String nickName,
            LocalDateTime orderedAt,
            LocalDateTime createdAt,
            String productName,
            ReviewReplyDTO reply,
            List<String> imageUrls
    ) {
        private ReviewStoreItemBuilder(
                final Long reviewId,
                final Integer rating,
                final String description,
                final String nickName,
                final LocalDateTime orderedAt,
                final LocalDateTime createdAt,
                final String productName,
                final ReviewReplyDTO reply
        ) {
            this(reviewId, rating, description, nickName, orderedAt, createdAt, productName, reply, new ArrayList<>());
        }

        private ReviewStoreItemDTO toDto() {
            return new ReviewStoreItemDTO(
                    reviewId,
                    rating,
                    description,
                    imageUrls,
                    nickName,
                    orderedAt,
                    createdAt,
                    productName,
                    reply
            );
        }
    }
}
