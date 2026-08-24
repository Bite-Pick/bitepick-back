package com.magambell.server.review.domain.repository;

import com.magambell.server.review.app.port.in.request.ReviewReportListServiceRequest;
import com.magambell.server.review.app.port.out.response.ReviewReportListDTO;
import com.magambell.server.review.domain.entity.ReviewReport;
import com.magambell.server.review.domain.enums.ReviewStatus;
import com.magambell.server.user.domain.enums.UserStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.magambell.server.review.domain.entity.QReview.review;
import static com.magambell.server.review.domain.entity.QReviewReport.reviewReport;
import static com.magambell.server.user.domain.entity.QUser.user;


@RequiredArgsConstructor
public class ReviewReportRepositoryImpl implements ReviewReportRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ReviewReportListDTO> getReviewReportList(final ReviewReportListServiceRequest request, final Pageable pageable) {
        BooleanBuilder conditions = new BooleanBuilder();
        conditions.and(reviewReport.id.eq(request.reviewId()));
        conditions.and(user.userStatus.eq(UserStatus.ACTIVE));
        conditions.and(review.reviewStatus.eq(ReviewStatus.ACTIVE));
        return queryFactory
                .select(
                        Projections.constructor(
                                ReviewReportListDTO.class,
                                reviewReport.user.id,
                                user.nickName,
                                user.userRole
                        )
                )
                .from(reviewReport)
                .innerJoin(user).on(user.id.eq(reviewReport.user.id))
                .where(conditions)
                .orderBy(reviewReport.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public ReviewReport getReviewReportByReviewIdAndUserId(Long reviewId, Long userId) {
        return queryFactory
                .select(reviewReport)
                .from(reviewReport)
                .where(reviewReport.review.id.eq(reviewId)
                        .and(reviewReport.user.id.eq(userId)))
                .fetchOne();
    }

}
