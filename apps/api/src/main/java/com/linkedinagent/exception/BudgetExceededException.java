package com.linkedinagent.exception;

public class BudgetExceededException extends ApiException {

    public BudgetExceededException(String message) {
        super(message, ErrorCode.GEMINI_DAILY_LIMIT_REACHED);
    }
}
