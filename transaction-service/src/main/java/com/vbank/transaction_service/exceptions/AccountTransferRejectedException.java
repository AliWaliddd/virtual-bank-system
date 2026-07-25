package com.vbank.transaction_service.exceptions;

/**
 * Represents a business-level rejection from Account Service,
 * such as insufficient funds, an inactive account, or an
 * invalid transfer operation.
 */
public class AccountTransferRejectedException extends RuntimeException {

    public AccountTransferRejectedException(String message) {
        super(message);
    }
}