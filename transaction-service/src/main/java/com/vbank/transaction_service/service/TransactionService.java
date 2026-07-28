package com.vbank.transaction_service.service;

import com.vbank.transaction_service.dto.TransactionResponse;
import com.vbank.transaction_service.dto.TransferExecutionRequest;
import com.vbank.transaction_service.dto.TransferInitiationRequest;
import com.vbank.transaction_service.model.RequestContext;

import java.util.List;
import java.util.UUID;

public interface TransactionService {

    TransactionResponse initiateTransfer(
            TransferInitiationRequest request,
            RequestContext requestContext
    );

    TransactionResponse executeTransfer(
            TransferExecutionRequest request,
            RequestContext requestContext
    );

    List<TransactionResponse> getTransactions(
            UUID accountId,
            RequestContext requestContext
    );
}
