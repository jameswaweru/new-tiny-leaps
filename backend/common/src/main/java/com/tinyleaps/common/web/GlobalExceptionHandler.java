package com.tinyleaps.common.web;

import com.tinyleaps.common.api.ApiException;
import com.tinyleaps.common.api.ApiResponse;
import com.tinyleaps.common.logging.TLLog;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/** Maps every error to the unified response envelope. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApi(ApiException e) {
        TLLog.warn("Business error: " + e.getDescription());
        return ResponseEntity.status(e.getStatusCode())
                .body(ApiResponse.error(e.getStatusCode(), e.getCustomerMessage(), e.getDescription()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException e) {
        String details = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "Something is missing. Please check and try again.", details));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MissingRequestHeaderException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(Exception e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "Something is missing. Please check and try again.", e.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity.status(404)
                .body(ApiResponse.error(404, "We couldn't find what you were looking for.", e.getMessage()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethod(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(405)
                .body(ApiResponse.error(405, "That action isn't supported.", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception e) {
        TLLog.error("Unexpected error", e);
        return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "Oops! Something went wrong. Please try again.", "Internal server error"));
    }
}
