package com.magambell.server.review.app.service;

import com.magambell.server.common.enums.ErrorCode;
import com.magambell.server.common.exception.DuplicateException;
import com.magambell.server.common.exception.InvalidRequestException;
import com.magambell.server.order.app.port.out.OrderQueryPort;
import com.magambell.server.order.domain.entity.OrderGoods;
import com.magambell.server.order.domain.enums.OrderStatus;
import com.magambell.server.review.app.port.in.ReviewUseCase;
import com.magambell.server.review.app.port.in.dto.ReportReviewDTO;
import com.magambell.server.review.app.port.in.request.*;
import com.magambell.server.review.app.port.out.ReviewCommandPort;
import com.magambell.server.review.app.port.out.ReviewQueryPort;
import com.magambell.server.review.app.port.out.response.ReviewListDTO;
import com.magambell.server.review.app.port.out.response.ReviewRatingSummaryDTO;
import com.magambell.server.review.app.port.out.response.ReviewRegisterResponseDTO;
import com.magambell.server.review.app.port.out.response.ReviewReportListDTO;
import com.magambell.server.review.domain.entity.Review;
import com.magambell.server.user.app.port.out.UserQueryPort;
import com.magambell.server.user.domain.entity.User;
import com.magambell.server.user.domain.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class ReviewService implements ReviewUseCase {

    private final ReviewCommandPort reviewCommandPort;
    private final ReviewQueryPort reviewQueryPort;
    private final UserQueryPort userQueryPort;
    private final OrderQueryPort orderQueryPort;

    @Transactional
    @Override
    public ReviewRegisterResponseDTO registerReview(final RegisterReviewServiceRequest request, final Long userId) {
        User user = userQueryPort.findById(userId);
        OrderGoods orderGoods = orderQueryPort.findOrderGoodsById(request.orderGoodsId());

        validateOrderStatus(orderGoods);
        existsOrderReview(orderGoods, user);

        return reviewCommandPort.registerReview(request.toDto(user, orderGoods));
    }

    @Override
    public List<ReviewListDTO> getReviewList(final ReviewListServiceRequest request) {
        return reviewQueryPort.getReviewList(request, PageRequest.of(request.page() - 1, request.size()));
    }

    @Override
    public ReviewRatingSummaryDTO getReviewRatingAll(final ReviewRatingAllServiceRequest request) {
        return reviewQueryPort.getReviewRatingAll(request);
    }

    @Override
    public List<ReviewListDTO> getReviewListByUser(final ReviewMyServiceRequest request) {
        User user = userQueryPort.findById(request.userId());
        return reviewQueryPort.getReviewListByUser(user, PageRequest.of(request.page() - 1, request.size()));
    }

    @Transactional
    @Override
    public void deleteReview(final DeleteReviewServiceRequest request) {
        User user = userQueryPort.findById(request.userId());
        Review review = reviewQueryPort.findByIdAndUserId(request.reviewId(), user.getId());
        review.delete();
    }

    @Transactional
    @Override
    public void registerReviewReply(final RegisterReviewReplyServiceRequest request) {
        validateReplyContent(request.content());

        User user = userQueryPort.findById(request.userId());
        Review review = reviewQueryPort.findById(request.reviewId());

        validateReviewOwner(user, review);
        reviewCommandPort.saveReviewReply(review.addReviewReply(request.content().trim()));
    }

    @Transactional
    @Override
    public void deleteReviewReply(final DeleteReviewReplyServiceRequest request) {
        User user = userQueryPort.findById(request.userId());
        Review review = reviewQueryPort.findById(request.reviewId());

        validateReviewOwner(user, review);
        review.deleteReviewReply();
    }

    @Transactional
    @Override
    public void reportReview(final ReportReviewServiceRequest request) {
        User user = userQueryPort.findById(request.userId());
        Review review = reviewQueryPort.findById(request.reviewId());

        // 여기부터 하기
        reviewCommandPort.reportReview(new ReportReviewDTO(review, user));
    }

    @Override
    public List<ReviewReportListDTO> getReviewReportList(final ReviewReportListServiceRequest request) {
        return reviewQueryPort.getReviewReportList(request, PageRequest.of(request.page() - 1, request.size()));
    }

    private void validateOrderStatus(final OrderGoods orderGoods) {
        System.out.println(orderGoods.getOrder().getOrderStatus());
        if (orderGoods.getOrder().getOrderStatus() != OrderStatus.COMPLETED) {
            throw new InvalidRequestException(ErrorCode.INVALID_ORDER_STATUS_COMPLETED);
        }
    }

    private void existsOrderReview(final OrderGoods orderGoods, final User user) {
        if (reviewQueryPort.existsOrderAndReview(orderGoods, user)) {
            throw new DuplicateException(ErrorCode.DUPLICATE_REVIEW);
        }
    }

    private void validateReviewOwner(final User user, final Review review) {
        if (user.getUserRole() != UserRole.OWNER || !review.getOrderGoods().getGoods().getStore().isOwnedBy(user)) {
            throw new InvalidRequestException(ErrorCode.INVALID_STORE_OWNER);
        }
    }

    private void validateReplyContent(final String content) {
        if (content == null || content.isBlank() || content.length() > 500) {
            throw new InvalidRequestException(ErrorCode.INVALID_REVIEW_REPLY_CONTENT);
        }
    }
}
