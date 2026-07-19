package com.vbank.transaction_service.exceptions;

public class TransactionAlreadyProcessedException extends RuntimeException {

    public TransactionAlreadyProcessedException() {
        super("Transaction has already been processed.");
    }
}
