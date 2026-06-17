package com.linkedinagent.exception;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(message, ErrorCode.NOT_FOUND);
    }
}
