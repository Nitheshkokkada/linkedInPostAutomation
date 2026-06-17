package com.linkedinagent.exception;

public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(message, ErrorCode.CONFLICT);
    }
}
