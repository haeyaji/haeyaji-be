package com.haeyaji.be.common.response;

import static org.springframework.http.HttpStatus.*;

import com.haeyaji.be.common.exception.ResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessCode implements ResponseCode {

    GET_SUCCESS(OK, "조회에 성공했습니다."),
    POST_SUCCESS(CREATED, "생성에 성공했습니다."),
    PUT_SUCCESS(OK, "수정에 성공했습니다."),
    DELETE_SUCCESS(OK, "삭제에 성공했습니다.");

    private final HttpStatus status;
    private final String message;
}
