package com.magambell.server.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.magambell.server.common.ErrorResponse;
import com.magambell.server.common.enums.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;

class ExceptionResolverTest {

    private final ExceptionResolver exceptionResolver = new ExceptionResolver();

    @DisplayName("권한이 없는 요청은 403 응답으로 처리한다.")
    @Test
    void handleAccessDeniedException() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/order");
        AccessDeniedException exception = new AuthorizationDeniedException(
                "Access Denied", new AuthorizationDecision(false));

        // when
        ResponseEntity<ErrorResponse> response =
                exceptionResolver.handleAccessDeniedException(exception, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getBody().getName()).isEqualTo(AccessDeniedException.class.getSimpleName());
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.ACCESS_DENIED.name());
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.ACCESS_DENIED.getMessage());
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/order");
    }
}
