package com.vbank.transaction_service.exceptions;



public class InvalidTransferException extends RuntimeException {

    public InvalidTransferException(String message) {
        super(message);
    }
}