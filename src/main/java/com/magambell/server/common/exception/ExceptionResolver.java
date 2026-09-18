package com.magambell.server.common.exception;

import com.magambell.server.common.ErrorResponse;
import com.magambell.server.common.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExceptionResolver {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e, HttpServletRequest request) {
        log.error("CustomException: {}", e.getMessage());

        return ResponseEntity
                .status(e.getStatusCode())
                .body(ErrorResponse.builder()
                        .statusCode(e.getStatusCode().value())
                        .name(e.getName())
                        .code(e.getErrorCode())
                        .message(e.getMessage())
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(org.springframework.validation.BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(org.springframework.validation.BindException e,
                                                             HttpServletRequest request) {
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("BindException: {}", errorMessage);

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.builder()
                        .statusCode(400)
                        .code("VALIDATION_ERROR")
                        .message(errorMessage)
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadableException(HttpMessageNotReadableException e,
                                                                            HttpServletRequest request) {
        log.warn("Malformed request body: {}", e.getMessage());

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.builder()
                        .statusCode(400)
                        .code("INVALID_REQUEST_BODY")
                        .message("요청 본문 값이 올바르지 않습니다.")
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e,
                                                                     HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
        log.warn("Access denied: method={}, path={}, message={}",
                request.getMethod(), request.getRequestURI(), e.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.builder()
                        .statusCode(HttpStatus.FORBIDDEN.value())
                        .name(AccessDeniedException.class.getSimpleName())
                        .code(errorCode.name())
                        .message(errorCode.getMessage())
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .name(e.getClass().getSimpleName())
                        .code("INTERNAL_SERVER_ERROR")
                        .message("서버 오류가 발생했습니다. 관리자에게 문의해주세요.")
                        .path(request.getRequestURI())
                        .build());
    }
}
