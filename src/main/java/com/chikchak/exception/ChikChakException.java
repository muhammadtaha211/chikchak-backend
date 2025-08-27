package com.chikchak.exception;

import org.springframework.http.HttpStatus;

// Custom Exception
public class ChikChakException extends RuntimeException {
    private final HttpStatus status;

    public ChikChakException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public ChikChakException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public ChikChakException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public HttpStatus getStatus() {
        return status;
    }
}






