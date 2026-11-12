package com.tinyleaps.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Unified response envelope returned by every Tiny Leaps API.
 * <pre>{ statusCode, customerMessage, description, data, pagination }</pre>
 * {@code data} may be an object or an array; {@code pagination} is only present on paginated endpoints.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"statusCode", "customerMessage", "description", "data", "pagination"})
public record ApiResponse<T>(int statusCode,
                             String customerMessage,
                             String description,
                             T data,
                             Pagination pagination) {

    public static <T> ApiResponse<T> ok(String customerMessage, String description, T data) {
        return new ApiResponse<>(200, customerMessage, description, data, null);
    }

    public static <T> ApiResponse<T> created(String customerMessage, String description, T data) {
        return new ApiResponse<>(201, customerMessage, description, data, null);
    }

    public static <T> ApiResponse<T> page(String customerMessage, String description, T data, Pagination pagination) {
        return new ApiResponse<>(200, customerMessage, description, data, pagination);
    }

    public static ApiResponse<Object> error(int statusCode, String customerMessage, String description) {
        return new ApiResponse<>(statusCode, customerMessage, description, null, null);
    }
}
