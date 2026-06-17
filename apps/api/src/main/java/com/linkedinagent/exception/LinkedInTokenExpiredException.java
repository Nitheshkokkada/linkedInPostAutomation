package com.linkedinagent.exception;

public class LinkedInTokenExpiredException extends LinkedInApiException {

    public LinkedInTokenExpiredException(String message) {
        super(message);
    }

    public LinkedInTokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
