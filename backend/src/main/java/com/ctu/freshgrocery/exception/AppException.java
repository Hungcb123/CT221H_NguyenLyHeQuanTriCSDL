package com.ctu.freshgrocery.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final Integer statusCode;

    public AppException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.statusCode = status.value();
    }

    public AppException(HttpStatus status, Integer statusCode, String message) {
        super(message);
        this.status = status;
        this.statusCode = statusCode;
    }
}
