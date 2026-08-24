package com.magambell.server.review.domain.repository;

import com.magambell.server.review.app.port.in.request.ReviewListServiceRequest;
import com.magambell.server.review.app.port.in.request.ReviewRatingAllServiceRequest;
import com.magambell.server.review.app.port.in.request.ReviewStoreServiceRequest;
import com.magambell.server.review.app.port.out.response.ReviewListDTO;
import com.magambell.server.review.app.port.out.response.ReviewRatingSummaryDTO;
import com.magambell.server.review.app.port.out.response.ReviewStoreItemDTO;
import com.magambell.server.review.app.port.out.response.ReviewStoreSummaryDTO;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface ReviewRepositoryCustom {
    List<ReviewListDTO> getReviewList(ReviewListServiceRequest request, Pageable pageable);

    ReviewRatingSummaryDTO getReviewRatingAll(ReviewRatingAllServiceRequest request);

    List<ReviewListDTO> getReviewListByUser(Long userId, Pageable pageable);

    List<ReviewStoreItemDTO> getStoreReviewList(ReviewStoreServiceRequest request, Long storeId);

    ReviewStoreSummaryDTO getStoreReviewSummary(Long storeId);
}
