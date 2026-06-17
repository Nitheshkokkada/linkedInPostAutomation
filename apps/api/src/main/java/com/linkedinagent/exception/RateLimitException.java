package com.linkedinagent.exception;

public class RateLimitException extends ApiException {

    public RateLimitException(String message) {
        super(message, ErrorCode.RATE_LIMITED);
    }
}
