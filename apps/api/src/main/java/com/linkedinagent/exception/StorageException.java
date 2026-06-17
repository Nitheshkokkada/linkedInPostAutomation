package com.linkedinagent.exception;

public class StorageException extends ApiException {

    public StorageException(String message) {
        super(message, ErrorCode.STORAGE_ERROR);
    }

    public StorageException(String message, Throwable cause) {
        super(message, ErrorCode.STORAGE_ERROR, cause);
    }
}
