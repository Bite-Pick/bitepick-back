package com.magambell.server.review.adapter.out.persistence;

import com.magambell.server.common.annotation.Adapter;
import com.magambell.server.common.enums.ErrorCode;
import com.magambell.server.common.exception.InvalidRequestException;
import com.magambell.server.common.s3.S3InputPort;
import com.magambell.server.common.s3.dto.ImageRegister;
import com.magambell.server.common.s3.dto.TransformedImageDTO;
import com.magambell.server.review.app.port.in.dto.RegisterReviewDTO;
import com.magambell.server.review.app.port.in.dto.ReportReviewDTO;
import com.magambell.server.review.app.port.out.ReviewCommandPort;
import com.magambell.server.review.app.port.out.response.ReviewPreSignedUrlImage;
import com.magambell.server.review.app.port.out.response.ReviewRegisterResponseDTO;
import com.magambell.server.review.domain.entity.Review;
import com.magambell.server.review.domain.entity.ReviewImage;
import com.magambell.server.review.domain.entity.ReviewReply;
import com.magambell.server.review.domain.entity.ReviewReport;
import com.magambell.server.review.domain.repository.ReviewReplyRepository;
import com.magambell.server.review.domain.repository.ReviewReportRepository;
import com.magambell.server.review.domain.repository.ReviewRepository;
import com.magambell.server.user.domain.entity.User;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Adapter
public class ReviewCommandAdapter implements ReviewCommandPort {

    private static final String IMAGE_PREFIX = "REVIEW";

    private final ReviewRepository reviewRepository;
    private final ReviewReplyRepository reviewReplyRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final S3InputPort s3InputPort;

    @Override
    public ReviewRegisterResponseDTO registerReview(final RegisterReviewDTO dto) {
        Review review = Review.create(dto);

        List<TransformedImageDTO> transformedImageDTOS = checkAndAddImages(dto.toImage(), dto.user());

        List<ReviewPreSignedUrlImage> reviewPreSignedUrlImages = addImagesAndGetPreSignedUrlImage(transformedImageDTOS,
                review);

        reviewRepository.save(review);

        return new ReviewRegisterResponseDTO(review.getId(), reviewPreSignedUrlImages);
    }

    @Override
    public void saveReviewReply(final ReviewReply reviewReply) {
        reviewReplyRepository.save(reviewReply);
    }

    @Override
    public void reportReview(ReportReviewDTO dto) {
        ReviewReport reviewReport = reviewReportRepository.getReviewReportByReviewIdAndUserId(dto.review().getId(), dto.user().getId());
        if(reviewReport != null) {
            throw new InvalidRequestException(ErrorCode.REVIEW_ALREADY_REPORTED);
        }

        reviewReportRepository.save(ReviewReport.create(dto));
    }

    private List<TransformedImageDTO> checkAndAddImages(final List<ImageRegister> image, final User user) {
        if (!image.isEmpty()) {
            return s3InputPort.saveImages(IMAGE_PREFIX,
                    image, user);
        }
        return List.of();
    }

    private List<ReviewPreSignedUrlImage> addImagesAndGetPreSignedUrlImage(final List<TransformedImageDTO> imageDTOList,
                                                                           final Review review) {
        return imageDTOList.stream()
                .map(imageDTO -> {
                    review.addReviewImage(ReviewImage.create(imageDTO.getUrl(), imageDTO.id()));
                    return new ReviewPreSignedUrlImage(imageDTO.id(), imageDTO.putUrl());
                })
                .toList();
    }
}
