package com.vbank.transaction_service.exceptions;

import java.util.UUID;

public class TransactionNotFoundException extends RuntimeException {
    public  TransactionNotFoundException(UUID transactionId){
        super("Transaction with id"+ transactionId+" was not found");
    }
}
