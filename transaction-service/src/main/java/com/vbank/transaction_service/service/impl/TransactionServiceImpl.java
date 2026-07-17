package com.vbank.transaction_service.service.impl;

import com.vbank.transaction_service.dto.TransactionResponse;
import com.vbank.transaction_service.dto.TransferExecutionRequest;
import com.vbank.transaction_service.dto.TransferInitiationRequest;
import com.vbank.transaction_service.entity.Transaction;
import com.vbank.transaction_service.enums.TransactionStatus;
import com.vbank.transaction_service.repository.TransactionRepository;
import com.vbank.transaction_service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    @Override
    public TransactionResponse initiateTransfer(TransferInitiationRequest request) {

        validateTransfer(request);

        Transaction transaction = Transaction.builder()
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .status(TransactionStatus.INITIATED)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        return mapToResponse(savedTransaction);
    }

    @Override
    public TransactionResponse executeTransfer(TransferExecutionRequest request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<TransactionResponse> getTransactions(UUID accountId) {

        List<Transaction> transactions = transactionRepository
                        .findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(
                                accountId,
                                accountId
                        );

        if (transactions.size() == 0){
            throw new IllegalArgumentException(
                    "No Account with this id"
            );
        }
        return transactions.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void validateTransfer(TransferInitiationRequest request) {

        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new IllegalArgumentException(
                    "Sender and receiver accounts cannot be the same."
            );
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