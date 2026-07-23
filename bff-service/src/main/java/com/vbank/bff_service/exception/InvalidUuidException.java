package com.vbank.bff_service.exception;

public class InvalidUuidException extends RuntimeException {

    public InvalidUuidException(String message) {
        super(message);
    }
}
