package com.magambell.server.review.adapter;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magambell.server.auth.app.service.JwtService;
import com.magambell.server.common.security.CustomUserDetails;
import com.magambell.server.review.adapter.in.web.EditReviewReplyRequest;
import com.magambell.server.review.app.port.in.ReviewUseCase;
import com.magambell.server.review.app.port.in.request.EditReviewReplyServiceRequest;
import com.magambell.server.user.domain.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewUseCase reviewUseCase;

    @MockBean
    private JwtService jwtService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @DisplayName("사장님은 리뷰 답글을 수정할 수 있다.")
    @Test
    void editReviewReply() throws Exception {
        // given
        Long reviewId = 1L;
        Long ownerId = 2L;
        EditReviewReplyRequest request = new EditReviewReplyRequest("수정된 답글");
        authenticateOwner(ownerId);

        // when // then
        mockMvc.perform(patch("/api/v1/review/{reviewId}/reply", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(reviewUseCase).editReviewReply(
                new EditReviewReplyServiceRequest(reviewId, ownerId, request.content()));
    }

    @DisplayName("리뷰 답글 수정 내용은 공백일 수 없다.")
    @Test
    void editReviewReplyWithBlankContent() throws Exception {
        // given
        authenticateOwner(2L);
        EditReviewReplyRequest request = new EditReviewReplyRequest("   ");

        // when // then
        mockMvc.perform(patch("/api/v1/review/{reviewId}/reply", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("리뷰 답글 수정 내용은 500자를 초과할 수 없다.")
    @Test
    void editReviewReplyWithTooLongContent() throws Exception {
        // given
        authenticateOwner(2L);
        EditReviewReplyRequest request = new EditReviewReplyRequest("a".repeat(501));

        // when // then
        mockMvc.perform(patch("/api/v1/review/{reviewId}/reply", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private void authenticateOwner(final Long ownerId) {
        CustomUserDetails ownerDetails = new CustomUserDetails(ownerId, UserRole.OWNER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ownerDetails, null, ownerDetails.getAuthorities()));
    }
}
