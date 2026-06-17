package com.linkedinagent.exception;

public class LinkedInApiException extends ApiException {

    public LinkedInApiException(String message) {
        super(message, ErrorCode.LINKEDIN_ERROR);
    }

    public LinkedInApiException(String message, Throwable cause) {
        super(message, ErrorCode.LINKEDIN_ERROR, cause);
    }
}
