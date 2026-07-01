package com.haeyaji.be.common.exception;

/**
 * 공통 에러 응답 바디 (camelCase — fe 폴백 처리용).
 */
public record ErrorResponse(String code, String message) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage());
    }
}
