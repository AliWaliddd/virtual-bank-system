package com.vbank.account_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_accounts_account_number",
                        columnNames = "account_number"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "account_id",
            nullable = false,
            updatable = false
    )
    private UUID accountId;

    @Column(
            name = "user_id",
            nullable = false,
            updatable = false
    )
    private UUID userId;

    @Column(
            name = "account_number",
            nullable = false,
            updatable = false,
            length = 10
    )
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "account_type",
            nullable = false,
            updatable = false,
            length = 20
    )
    private AccountType accountType;

    @Column(
            name = "balance",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private AccountStatus status;

    @Column(name = "last_transaction_at")
    private Instant lastTransactionAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    public Account(
            UUID userId,
            String accountNumber,
            AccountType accountType,
            BigDecimal initialBalance
    ) {
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = initialBalance.setScale(2, RoundingMode.UNNECESSARY);
        this.status = AccountStatus.ACTIVE;
        this.lastTransactionAt = null;
    }

    public void debit(
            BigDecimal amount,
            Instant transactionTime
    ) {
        balance = balance.subtract(amount);
        lastTransactionAt = transactionTime;
    }

    public void credit(
            BigDecimal amount,
            Instant transactionTime
    ) {
        balance = balance.add(amount);
        lastTransactionAt = transactionTime;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
