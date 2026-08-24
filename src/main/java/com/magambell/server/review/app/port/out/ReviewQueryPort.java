package com.magambell.server.review.app.port.out;

import com.magambell.server.order.domain.entity.OrderGoods;
import com.magambell.server.review.app.port.in.request.ReviewListServiceRequest;
import com.magambell.server.review.app.port.in.request.ReviewRatingAllServiceRequest;
import com.magambell.server.review.app.port.in.request.ReviewReportListServiceRequest;
import com.magambell.server.review.app.port.in.request.ReviewStoreServiceRequest;
import com.magambell.server.review.app.port.out.response.ReviewListDTO;
import com.magambell.server.review.app.port.out.response.ReviewRatingSummaryDTO;
import com.magambell.server.review.app.port.out.response.ReviewReportListDTO;
import com.magambell.server.review.app.port.out.response.ReviewStoreItemDTO;
import com.magambell.server.review.app.port.out.response.ReviewStoreSummaryDTO;
import com.magambell.server.review.domain.entity.Review;
import com.magambell.server.user.domain.entity.User;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface ReviewQueryPort {
    boolean existsOrderAndReview(OrderGoods orderGoods, User user);

    List<ReviewListDTO> getReviewList(ReviewListServiceRequest request, Pageable pageable);

    ReviewRatingSummaryDTO getReviewRatingAll(ReviewRatingAllServiceRequest request);

    List<ReviewListDTO> getReviewListByUser(User user, Pageable pageable);

    List<ReviewStoreItemDTO> getStoreReviewList(ReviewStoreServiceRequest request, Long storeId);

    ReviewStoreSummaryDTO getStoreReviewSummary(Long storeId);

    Review findByIdAndUserId(Long reviewId, Long userId);

    Review findById(Long reviewId);

    List<ReviewReportListDTO> getReviewReportList(ReviewReportListServiceRequest request, Pageable pageable);

}
