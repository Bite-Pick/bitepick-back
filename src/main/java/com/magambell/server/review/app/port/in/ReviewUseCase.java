package com.magambell.server.review.app.port.in;

import com.magambell.server.review.app.port.in.request.*;
import com.magambell.server.review.app.port.out.response.ReviewListDTO;
import com.magambell.server.review.app.port.out.response.ReviewRatingSummaryDTO;
import com.magambell.server.review.app.port.out.response.ReviewRegisterResponseDTO;
import com.magambell.server.review.app.port.out.response.ReviewReportListDTO;

import java.util.List;

public interface ReviewUseCase {
    ReviewRegisterResponseDTO registerReview(RegisterReviewServiceRequest request, Long userId);

    List<ReviewListDTO> getReviewList(ReviewListServiceRequest request);

    ReviewRatingSummaryDTO getReviewRatingAll(ReviewRatingAllServiceRequest request);

    List<ReviewListDTO> getReviewListByUser(ReviewMyServiceRequest request);

    void deleteReview(DeleteReviewServiceRequest request);

    void registerReviewReply(RegisterReviewReplyServiceRequest request);

    void deleteReviewReply(DeleteReviewReplyServiceRequest request);

    void reportReview(ReportReviewServiceRequest request);

    List<ReviewReportListDTO> getReviewReportList(ReviewReportListServiceRequest request);


}
