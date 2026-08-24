package com.magambell.server.review.adapter;

import com.magambell.server.common.Response;
import com.magambell.server.common.security.CustomUserDetails;
import com.magambell.server.common.swagger.BaseResponse;
import com.magambell.server.review.adapter.in.web.*;
import com.magambell.server.review.adapter.out.persistence.ReviewListResponse;
import com.magambell.server.review.adapter.out.persistence.ReviewRatingSummaryResponse;
import com.magambell.server.review.adapter.out.persistence.ReviewRegisterResponse;
import com.magambell.server.review.adapter.out.persistence.ReviewReportListResponse;
import com.magambell.server.review.adapter.out.persistence.ReviewStoreResponse;
import com.magambell.server.review.app.port.in.ReviewUseCase;
import com.magambell.server.review.app.port.in.request.DeleteReviewReplyServiceRequest;
import com.magambell.server.review.app.port.in.request.DeleteReviewServiceRequest;
import com.magambell.server.review.app.port.in.request.ReportReviewServiceRequest;
import com.magambell.server.review.app.port.out.response.ReviewListDTO;
import com.magambell.server.review.app.port.out.response.ReviewRatingSummaryDTO;
import com.magambell.server.review.app.port.out.response.ReviewRegisterResponseDTO;
import com.magambell.server.review.app.port.out.response.ReviewReportListDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "Review", description = "Review API")
@RequiredArgsConstructor
@RequestMapping("/api/v1/review")
@RestController
public class ReviewController {

    private final ReviewUseCase reviewUseCase;

    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "리뷰 등록")
    @ApiResponse(responseCode = "200", content = {
            @Content(schema = @Schema(implementation = ReviewRegisterResponse.class))})
    @PostMapping("")
    public Response<ReviewRegisterResponse> registerReview(
            @RequestBody @Validated final RegisterReviewRequest request,
            @AuthenticationPrincipal final CustomUserDetails customUserDetails
    ) {
        ReviewRegisterResponseDTO reviewRegisterResponseDTO = reviewUseCase.registerReview(request.toServiceRequest(),
                customUserDetails.userId());

        return new Response<>(reviewRegisterResponseDTO.toResponse());
    }

    @Operation(summary = "리뷰 리스트")
    @ApiResponse(responseCode = "200", content = {
            @Content(schema = @Schema(implementation = ReviewListResponse.class))})
    @GetMapping("")
    public Response<ReviewListResponse> getReviewList(
            @ModelAttribute @Validated final ReviewListRequest request,
            @AuthenticationPrincipal final CustomUserDetails customUserDetails
    ) {
        Long userId = null;
        if(customUserDetails != null) {
            userId = customUserDetails.userId();
        }

        List<ReviewListDTO> reviewList = reviewUseCase.getReviewList(request.toServiceRequest(userId));
        return new Response<>(new ReviewListResponse(reviewList));
    }

    @Operation(summary = "리뷰 리스트 평점별 조회")
    @ApiResponse(responseCode = "200", content = {
            @Content(schema = @Schema(implementation = ReviewRatingSummaryResponse.class))})
    @GetMapping("/rating")
    public Response<ReviewRatingSummaryResponse> getReviewRatingAll(
            @ModelAttribute @Validated final ReviewRatingAllRequest request
    ) {
        ReviewRatingSummaryDTO reviewRatingAll = reviewUseCase.getReviewRatingAll(request.toServiceRequest());
        return new Response<>(reviewRatingAll.toResponse());
    }

    @Operation(summary = "내가 작성한 리뷰 목록")
    @ApiResponse(responseCode = "200", content = {
            @Content(schema = @Schema(implementation = ReviewListResponse.class))})
    @GetMapping("/me")
    public Response<ReviewListResponse> getMyReviewList(
            @ModelAttribute @Validated final ReviewMyRequest request,
            @AuthenticationPrincipal final CustomUserDetails customUserDetails
    ) {
        List<ReviewListDTO> reviewListByUser = reviewUseCase.getReviewListByUser(
                request.toServiceRequest(customUserDetails.userId()));
        return new Response<>(new ReviewListResponse(reviewListByUser));
    }

    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "사장님 매장 리뷰 목록")
    @ApiResponse(responseCode = "200", content = {
            @Content(schema = @Schema(implementation = ReviewStoreResponse.class))})
    @GetMapping("/store")
    public Response<ReviewStoreResponse> getStoreReviewList(
            @ModelAttribute @Validated final ReviewStoreRequest request,
            @AuthenticationPrincipal final CustomUserDetails customUserDetails
    ) {
        return new Response<>(reviewUseCase.getStoreReviewList(request.toServiceRequest(customUserDetails.userId())));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "리뷰 삭제")
    @ApiResponse(responseCode = "200", content = {
            @Content(schema = @Schema(implementation = BaseResponse.class))})
    @DeleteMapping("/{reviewId}")
    public Response<BaseResponse> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal final CustomUserDetails customUserDetails
    ) {
        reviewUseCase.deleteReview(new DeleteReviewServiceRequest(reviewId, customUserDetails.userId()));

        return new Response<>();
    }

    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "리뷰 답글 등록")
    @ApiResponse(responseCode = "200", content = {
            @Content(schema = @Schema(implementation = BaseResponse.class))})
    @PostMapping("/{reviewId}/reply")
    public Response<BaseResponse> registerReviewReply(
            @PathVariable Long reviewId,
            @RequestBody @Validated final RegisterReviewReplyRequest request,
            @AuthenticationPrincipal final CustomUserDetails customUserDetails
    ) {
        reviewUseCase.registerReviewReply(request.toServiceRequest(reviewId, customUserDetails.userId()));

        return new Response<>();
    }

    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "리뷰 답글 삭제")
    @ApiResponse(responseCode = "200", content = {
            @Content(schema = @Schema(implementation = BaseResponse.class))})
    @DeleteMapping("/{reviewId}/reply")
    public Response<BaseResponse> deleteReviewReply(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal final CustomUserDetails customUserDetails
    ) {
        reviewUseCase.deleteReviewReply(new DeleteReviewReplyServiceRequest(reviewId, customUserDetails.userId()));

        return new Response<>();
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER')")
    @Operation(summary = "리뷰 신고")
    @ApiResponse(responseCode = "200", content = {
            @Content(schema = @Schema(implementation = BaseResponse.class))})
    @PostMapping("/report/{reviewId}")
    public Response<BaseResponse> reportReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal final CustomUserDetails customUserDetails
    ) {
        reviewUseCase.reportReview(new ReportReviewServiceRequest(reviewId, customUserDetails.userId()));

        return new Response<>();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "리뷰 신고 리스트")
    @ApiResponse(responseCode = "200", content = {
            @Content(schema = @Schema(implementation = ReviewReportListResponse.class))})
    @GetMapping("/report")
    public Response<ReviewReportListResponse> getReviewReportList(
            @ModelAttribute @Validated final ReviewReportListRequest request
    ) {
        List<ReviewReportListDTO> reviewReportList = reviewUseCase.getReviewReportList(request.toServiceRequest());
        return new Response<>(new ReviewReportListResponse(reviewReportList));
    }


    //  )
}
