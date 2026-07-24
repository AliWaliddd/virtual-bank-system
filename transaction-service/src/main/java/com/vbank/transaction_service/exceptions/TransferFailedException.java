package com.vbank.transaction_service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TransferFailedException extends RuntimeException {

    public TransferFailedException(String message) {
        super(message);
    }
}