package com.haeyaji.be.common.response;

import com.haeyaji.be.common.exception.ResponseCode;
import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        Instant timestamp
) {

    public static <T> ApiResponse<T> of(T data, ResponseCode responseCode) {
        return new ApiResponse<>(
                true,
                responseCode.getStatus().name(),
                responseCode.getMessage(),
                data,
                Instant.now()
        );
    }

    public static <T> ApiResponse<T> fail(T data, ResponseCode responseCode) {
        return new ApiResponse<>(
                false,
                responseCode.getStatus().name(),
                responseCode.getMessage(),
                data,
                Instant.now()
        );
    }

    public static <T> ApiResponse<T> fail(ResponseCode responseCode) {
        return new ApiResponse<>(
                false,
                responseCode.getStatus().name(),
                responseCode.getMessage(),
                null,
                Instant.now()
        );
    }

}
