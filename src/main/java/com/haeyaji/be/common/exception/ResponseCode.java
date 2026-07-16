package com.haeyaji.be.common.exception;

import org.springframework.http.HttpStatus;

public interface ResponseCode {
    HttpStatus getStatus();
    String getMessage();
}
