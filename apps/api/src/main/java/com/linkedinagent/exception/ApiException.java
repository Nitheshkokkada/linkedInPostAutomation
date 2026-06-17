package com.linkedinagent.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode code;

    public ApiException(String message, ErrorCode code) {
        super(message);
        this.code = code;
    }

    public ApiException(String message, ErrorCode code, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
