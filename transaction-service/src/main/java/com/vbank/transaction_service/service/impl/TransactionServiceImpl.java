package com.vbank.transaction_service.service.impl;

import com.vbank.transaction_service.client.AccountClient;
import com.vbank.transaction_service.dto.Request.TransferRequest;
import com.vbank.transaction_service.dto.TransactionResponse;
import com.vbank.transaction_service.dto.TransferExecutionRequest;
import com.vbank.transaction_service.dto.TransferInitiationRequest;
import com.vbank.transaction_service.entity.Transaction;
import com.vbank.transaction_service.enums.TransactionStatus;
import com.vbank.transaction_service.exceptions.AccountServiceBadGatewayException;
import com.vbank.transaction_service.exceptions.AccountServiceUnavailableException;
import com.vbank.transaction_service.exceptions.AccountTransferConflictException;
import com.vbank.transaction_service.exceptions.AccountTransferRejectedException;
import com.vbank.transaction_service.exceptions.InvalidTransferException;
import com.vbank.transaction_service.exceptions.TransactionAlreadyProcessedException;
import com.vbank.transaction_service.exceptions.TransactionNotFoundException;
import com.vbank.transaction_service.model.RequestContext;
import com.vbank.transaction_service.repository.TransactionRepository;
import com.vbank.transaction_service.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;

    public TransactionServiceImpl(
            TransactionRepository transactionRepository,
            AccountClient accountClient
    ) {
        this.transactionRepository = transactionRepository;
        this.accountClient = accountClient;
    }

    @Override
    public TransactionResponse initiateTransfer(
            TransferInitiationRequest request,
            RequestContext requestContext
    ) {
        validateTransfer(request);

        validateAccountExists(
                request.getFromAccountId(),
                requestContext
        );
        validateAccountExists(
                request.getToAccountId(),
                requestContext
        );

        Transaction transaction = Transaction.builder()
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .status(TransactionStatus.INITIATED)
                .build();

        Transaction savedTransaction =
                transactionRepository.saveAndFlush(transaction);

        return mapToResponse(savedTransaction);
    }

    @Override
    public TransactionResponse executeTransfer(
            TransferExecutionRequest request,
            RequestContext requestContext
    ) {
        Transaction transaction = transactionRepository
                .findById(request.getTransactionId())
                .orElseThrow(() ->
                        new TransactionNotFoundException(
                                request.getTransactionId()
                        )
                );

        if (transaction.getStatus() != TransactionStatus.INITIATED) {
            throw new TransactionAlreadyProcessedException();
        }

        TransferRequest transferRequest = new TransferRequest(
                transaction.getFromAccountId(),
                transaction.getToAccountId(),
                transaction.getAmount()
        );

        try {
            accountClient.transfer(
                    transferRequest,
                    requestContext
            );
        } catch (AccountTransferRejectedException exception) {
            markTransactionAsFailed(transaction, exception);
            throw exception;
        } catch (
                AccountTransferConflictException
                | AccountServiceUnavailableException
                | AccountServiceBadGatewayException exception
        ) {
            LOGGER.warn(
                    "Transaction {} remains INITIATED because Account Service returned a temporary or ambiguous failure: {}",
                    transaction.getId(),
                    exception.getMessage()
            );

            throw exception;
        }

        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setFailureReason(null);
        transaction.setExecutedAt(Instant.now());

        Transaction savedTransaction =
                transactionRepository.saveAndFlush(transaction);

        return mapToResponse(savedTransaction);
    }

    @Override
    public List<TransactionResponse> getTransactions(
            UUID accountId,
            RequestContext requestContext
    ) {
        validateAccountExists(accountId, requestContext);

        return transactionRepository
                .findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(
                        accountId,
                        accountId
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void markTransactionAsFailed(
            Transaction transaction,
            RuntimeException transferException
    ) {
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setExecutedAt(Instant.now());
        transaction.setFailureReason(
                safeFailureReason(transferException)
        );

        try {
            transactionRepository.saveAndFlush(transaction);
        } catch (RuntimeException persistenceException) {
            transferException.addSuppressed(persistenceException);

            LOGGER.error(
                    "Could not persist FAILED status for transaction {}.",
                    transaction.getId(),
                    persistenceException
            );
        }
    }

    private String safeFailureReason(RuntimeException exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        int maximumLength = 500;

        return message.length() <= maximumLength
                ? message
                : message.substring(0, maximumLength);
    }

    private void validateTransfer(
            TransferInitiationRequest request
    ) {
        if (request.getFromAccountId().equals(
                request.getToAccountId()
        )) {
            throw new InvalidTransferException(
                    "Sender and receiver accounts cannot be the same."
            );
        }
    }

    private void validateAccountExists(
            UUID accountId,
            RequestContext requestContext
    ) {
        try {
            accountClient.getAccount(accountId, requestContext);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new InvalidTransferException(
                    "Account does not exist."
            );
        }
    }

    private TransactionResponse mapToResponse(
            Transaction transaction
    ) {
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
