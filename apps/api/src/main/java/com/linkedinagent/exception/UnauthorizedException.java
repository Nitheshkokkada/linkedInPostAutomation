package com.linkedinagent.exception;

public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(message, ErrorCode.UNAUTHORIZED);
    }
}
