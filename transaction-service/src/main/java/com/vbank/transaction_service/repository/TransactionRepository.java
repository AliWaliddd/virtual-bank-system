package com.vbank.transaction_service.repository;

import com.vbank.transaction_service.entity.Transaction;
import com.vbank.transaction_service.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(
            UUID fromAccountId,
            UUID toAccountId
    );

    List<Transaction> findByStatus(TransactionStatus status);
}