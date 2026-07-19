package com.vbank.account_service.repository;

import com.vbank.account_service.entity.Account;
import com.vbank.account_service.entity.AccountStatus;
import com.vbank.account_service.entity.AccountType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository
        extends JpaRepository<Account, UUID> {

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findAllByUserIdOrderByCreatedAtAsc(
            UUID userId
    );

    /*
     * Used by transfers and explicit account activation.
     *
     * PESSIMISTIC_WRITE prevents another transaction from modifying
     * the same account until the current transaction finishes.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select account
            from Account account
            where account.accountId = :accountId
            """)
    Optional<Account> findByIdForUpdate(
            @Param("accountId") UUID accountId
    );

    /*
     * Marks an ACTIVE account as INACTIVE when:
     *
     * 1. The account is not a SYSTEM account.
     * 2. At least one relevant timestamp exists.
     * 3. lastTransactionAt is either null or older than the cutoff.
     * 4. reactivatedAt is either null or older than the cutoff.
     *
     * Conditions 3 and 4 together mean that the latest existing
     * relevant timestamp must be older than the inactivity cutoff.
     *
     * Examples:
     *
     * Old transaction + recent reactivation -> remains ACTIVE.
     * Recent transaction + old reactivation -> remains ACTIVE.
     * Old transaction + old reactivation -> becomes INACTIVE.
     * Null transaction + old reactivation -> becomes INACTIVE.
     * Old transaction + null reactivation -> becomes INACTIVE.
     * Both timestamps null -> remains ACTIVE.
     */
    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
            update Account account
               set account.status = :inactiveStatus,
                   account.updatedAt = :updatedAt
             where account.status = :activeStatus
               and account.accountType <> :excludedAccountType
               and (
                       account.lastTransactionAt is not null
                       or account.reactivatedAt is not null
                   )
               and (
                       account.lastTransactionAt is null
                       or account.lastTransactionAt < :cutoff
                   )
               and (
                       account.reactivatedAt is null
                       or account.reactivatedAt < :cutoff
                   )
            """)
    int markStaleAccountsInactive(
            @Param("activeStatus")
            AccountStatus activeStatus,

            @Param("inactiveStatus")
            AccountStatus inactiveStatus,

            @Param("excludedAccountType")
            AccountType excludedAccountType,

            @Param("cutoff")
            Instant cutoff,

            @Param("updatedAt")
            Instant updatedAt
    );
}