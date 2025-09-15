package com.network.digitaltwin.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic API response wrapper for all endpoints.
 * 
 * @param status indicates whether the request was successful
 * @param message a descriptive message about the response
 * @param data the payload data, if any
 * @param correlationId unique identifier for tracking requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean status;
    private String message;
    private T data;
    private String correlationId;

    /**
     * Creates a successful response with data.
     * @param data the payload data
     * @param <T> the type of data
     * @return ApiResponse with success status
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(true)
                .message("Success")
                .data(data)
                .correlationId(UUID.randomUUID().toString())
                .build();
    }

    /**
     * Creates a successful response without data.
     * @param message success message
     * @return ApiResponse with success status
     */
    public static ApiResponse<Void> success(String message) {
        return ApiResponse.<Void>builder()
                .status(true)
                .message(message)
                .correlationId(UUID.randomUUID().toString())
                .build();
    }

    /**
     * Creates an error response.
     * @param message error message
     * @return ApiResponse with error status
     */
    public static ApiResponse<Void> error(String message) {
        return ApiResponse.<Void>builder()
                .status(false)
                .message(message)
                .correlationId(UUID.randomUUID().toString())
                .build();
    }

    /**
     * Creates an error response with data.
     * @param message error message
     * @param data error details
     * @param <T> the type of error data
     * @return ApiResponse with error status
     */
    public static <T> ApiResponse<T> error(String message, T data) {
        return ApiResponse.<T>builder()
                .status(false)
                .message(message)
                .data(data)
                .correlationId(UUID.randomUUID().toString())
                .build();
    }
}
