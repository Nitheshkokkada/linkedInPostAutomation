package com.linkedinagent.exception;

public class AgentException extends ApiException {

    public AgentException(String message) {
        super(message, ErrorCode.AGENT_ERROR);
    }

    public AgentException(String message, Throwable cause) {
        super(message, ErrorCode.AGENT_ERROR, cause);
    }
}
