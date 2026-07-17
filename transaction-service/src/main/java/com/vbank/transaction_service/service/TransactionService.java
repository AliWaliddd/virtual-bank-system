package com.vbank.transaction_service.service;

import com.vbank.transaction_service.dto.TransactionResponse;
import com.vbank.transaction_service.dto.TransferExecutionRequest;
import com.vbank.transaction_service.dto.TransferInitiationRequest;

import java.util.List;
import java.util.UUID;

public interface TransactionService {

    TransactionResponse initiateTransfer(TransferInitiationRequest request);

    TransactionResponse executeTransfer(TransferExecutionRequest request);

    List<TransactionResponse> getTransactions(UUID accountId);
}