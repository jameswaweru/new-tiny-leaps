package com.tinyleaps.common.api;

/** Business exception mapped to the unified {@link ApiResponse} error envelope. */
public class ApiException extends RuntimeException {

    private final int statusCode;
    private final String customerMessage;

    public ApiException(int statusCode, String customerMessage, String description) {
        super(description);
        this.statusCode = statusCode;
        this.customerMessage = customerMessage;
    }

    public int getStatusCode() { return statusCode; }
    public String getCustomerMessage() { return customerMessage; }
    public String getDescription() { return getMessage(); }

    public static ApiException unauthorized(String description) {
        return new ApiException(401, "Your session has expired. Please sign in again.", description);
    }

    public static ApiException forbidden(String description) {
        return new ApiException(403, "You are not allowed to do that.", description);
    }

    public static ApiException notFound(String description) {
        return new ApiException(404, "We couldn't find what you were looking for.", description);
    }

    public static ApiException badRequest(String description) {
        return new ApiException(400, "Something is missing. Please check and try again.", description);
    }
}
