package com.vbank.account_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "accounts",
        indexes = {
                @Index(
                        name = "idx_accounts_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_accounts_status_last_transaction",
                        columnList = "status,last_transaction_at"
                ),
                @Index(
                        name = "idx_accounts_status_reactivated",
                        columnList = "status,reactivated_at"
                )
        }
)
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
            unique = true,
            length = 10,
            updatable = false
    )
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "account_type",
            nullable = false,
            length = 20,
            updatable = false
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

    /*
     * Stores the most recent explicit reactivation time.
     *
     * It does not replace lastTransactionAt because activating an
     * account is not a financial transaction.
     */
    @Column(name = "reactivated_at")
    private Instant reactivatedAt;

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

    protected Account() {
        // Required by JPA.
    }

    public Account(
            UUID userId,
            String accountNumber,
            AccountType accountType,
            BigDecimal balance
    ) {
        this.userId = Objects.requireNonNull(
                userId,
                "User ID must not be null."
        );
        this.accountNumber = Objects.requireNonNull(
                accountNumber,
                "Account number must not be null."
        );
        this.accountType = Objects.requireNonNull(
                accountType,
                "Account type must not be null."
        );
        this.balance = Objects.requireNonNull(
                balance,
                "Balance must not be null."
        );
        this.status = AccountStatus.ACTIVE;
    }

    public void debit(
            BigDecimal amount,
            Instant transactionTime
    ) {
        Objects.requireNonNull(
                amount,
                "Debit amount must not be null."
        );
        Objects.requireNonNull(
                transactionTime,
                "Transaction time must not be null."
        );

        balance = balance.subtract(amount);
        lastTransactionAt = transactionTime;
    }

    public void credit(
            BigDecimal amount,
            Instant transactionTime
    ) {
        Objects.requireNonNull(
                amount,
                "Credit amount must not be null."
        );
        Objects.requireNonNull(
                transactionTime,
                "Transaction time must not be null."
        );

        balance = balance.add(amount);
        lastTransactionAt = transactionTime;
    }

    public void activate(Instant activationTime) {
        Objects.requireNonNull(
                activationTime,
                "Activation time must not be null."
        );

        status = AccountStatus.ACTIVE;
        reactivatedAt = activationTime;
    }

    public void markInactive() {
        status = AccountStatus.INACTIVE;
    }

    @PrePersist
    private void onCreate() {
        Instant now = Instant.now();

        if (status == null) {
            status = AccountStatus.ACTIVE;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now();
    }
}