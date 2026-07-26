package com.vbank.transaction_service.service.impl;

import com.vbank.transaction_service.client.AccountClient;
import com.vbank.transaction_service.dto.*;
import com.vbank.transaction_service.dto.Request.TransferRequest;
import com.vbank.transaction_service.dto.TransactionResponse;
import com.vbank.transaction_service.dto.TransferExecutionRequest;
import com.vbank.transaction_service.dto.TransferInitiationRequest;
import com.vbank.transaction_service.entity.Transaction;
import com.vbank.transaction_service.enums.TransactionStatus;
import com.vbank.transaction_service.exceptions.InvalidTransferException;
import com.vbank.transaction_service.exceptions.TransactionAlreadyProcessedException;
import com.vbank.transaction_service.exceptions.TransactionNotFoundException;
import com.vbank.transaction_service.exceptions.TransferFailedException;
import com.vbank.transaction_service.repository.TransactionRepository;
import com.vbank.transaction_service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import com.vbank.transaction_service.exceptions.AccountTransferRejectedException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final LoggingProducerService loggingProducerService;

    @Override
    public TransactionResponse initiateTransfer(TransferInitiationRequest request) {

        validateTransfer(request);

        validateAccountsExist(request.getFromAccountId());
        validateAccountsExist(request.getToAccountId());

        Transaction transaction = Transaction.builder()
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .status(TransactionStatus.INITIATED)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        loggingProducerService.send(
                LogMessage.builder()
                        .message("Transaction initiated successfully.")
                        .messageType(MessageType.REQUEST)
                        .dateTime(Instant.now())
                        .serviceName("transaction-service")
                        .httpMethod("POST")
                        .path("/transactions/transfer/initiation")
                        .statusCode(200)
                        .correlationId(savedTransaction.getId())
                        .appName("Virtual Bank")
                        .build()
        );
        return mapToResponse(savedTransaction);
    }

    @Override
    public TransactionResponse executeTransfer(
            TransferExecutionRequest request
    ) {
        Transaction transaction = transactionRepository
                .findById(request.getTransactionId())
                .orElseThrow(() ->
                        new TransactionNotFoundException(
                                request.getTransactionId()
                        )
                );

        if (transaction.getStatus()
                != TransactionStatus.INITIATED) {

            throw new TransactionAlreadyProcessedException();
        }

        validateAccountsExist(
                transaction.getFromAccountId()
        );

        validateAccountsExist(
                transaction.getToAccountId()
        );

        TransferRequest transferRequest = new TransferRequest(
                transaction.getFromAccountId(),
                transaction.getToAccountId(),
                transaction.getAmount()
        );

        try {
            accountClient.transfer(transferRequest);

            transaction.setStatus(
                    TransactionStatus.SUCCESS
            );

            transaction.setFailureReason(null);
            transaction.setExecutedAt(Instant.now());

            Transaction savedTransaction=transactionRepository.save(transaction);
            loggingProducerService.send(
                    LogMessage.builder()
                            .message(e.getMessage())
                            .messageType(MessageType.REQUEST)
                            .dateTime(Instant.now())
                            .serviceName("transaction-service")
                            .httpMethod("POST")
                            .path("/transactions/transfer/execution")
                            .statusCode(500)
                            .correlationId(savedTransaction.getId())
                            .appName("Virtual Bank")
                            .build()
            );
            throw new TransferFailedException(e.getMessage());
                }

        transaction.setExecutedAt(Instant.now());
        Transaction saved = transactionRepository.save(transaction);
        loggingProducerService.send(
                LogMessage.builder()
                        .message("Transaction Executed successfully.")
                        .messageType(MessageType.REQUEST)
                        .dateTime(Instant.now())
                        .serviceName("transaction-service")
                        .httpMethod("POST")
                        .path("/transactions/transfer/execution")
                        .statusCode(200)
                        .correlationId(saved.getId())
                        .appName("Virtual Bank")
                        .build()
        );
        return mapToResponse(saved);
    }

    @Override
    public List<TransactionResponse> getTransactions(UUID accountId) {
        validateAccountsExist(accountId);
        List<Transaction> transactions = transactionRepository
                        .findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(
                                accountId,
                                accountId
                        );
        return transactions.stream()
                .map(this::mapToResponse)
                .toList();
    }

    //helpers
    private void validateTransfer(TransferInitiationRequest request) {

        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new InvalidTransferException(
                    "Sender and receiver accounts cannot be the same."
            );
        }
    }
    private void validateAccountsExist(UUID Id) {
        if (Id != null) {
            try {
                accountClient.getAccount(Id);
            } catch (HttpClientErrorException.NotFound ex) {
                throw new InvalidTransferException("account does not exist.");
            }
        }
    }

    private TransactionResponse mapToResponse(Transaction transaction) {

        return TransactionResponse.builder()
                .transactionId(transaction.getId())
                .fromAccountId(transaction.getFromAccountId())
                .toAccountId(transaction.getToAccountId())
                .amount(transaction.getAmount())
                .status(transaction.getStatus())
                .failureReason(transaction.getFailureReason())
                .createdAt(transaction.getCreatedAt())
                .executedAt(transaction.getExecutedAt())
                .build();
    }
}