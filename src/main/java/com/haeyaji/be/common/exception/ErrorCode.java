package com.haeyaji.be.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements ResponseCode {

    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 올바르지 않습니다."),
    WEATHER_UPSTREAM_ERROR(HttpStatus.BAD_GATEWAY, "날씨 정보를 가져오지 못했습니다."),
    NLP_UPSTREAM_ERROR(HttpStatus.BAD_GATEWAY, "추천 정보를 가져오지 못했습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP Method입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    MEMBER_NOT_FOUND(HttpStatus.BAD_REQUEST, "회원을 찾을 수 없습니다"),

    SELF_FRIEND_REQUEST_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자신에게 친구 요청을 보낼 수 없습니다."),
    ALREADY_FRIENDS(HttpStatus.CONFLICT, "이미 친구로 추가된 회원입니다."),
    DUPLICATE_FRIEND_REQUEST(HttpStatus.CONFLICT, "이미 보낸 요청입니다."),
    FRIEND_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "친구 요청을 찾을 수 없습니다."),
    FRIEND_REQUEST_FORBIDDEN(HttpStatus.FORBIDDEN, "본인에게 온 친구 요청만 처리할 수 있습니다."),

    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),

    PAST_DATE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "과거 날짜에는 할 일을 추가할 수 없습니다."),

    REQUEST_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "이미 처리된 요청입니다."),

    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 알림이 존재하지 않습니다."),
    NOTIFICATION_FORBIDDEN(HttpStatus.FORBIDDEN, "자신의 알림만 확인할 수 있습니다."),

    ;

    private final HttpStatus status;
    private final String message;
}
