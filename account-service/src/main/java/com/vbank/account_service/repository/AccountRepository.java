package com.vbank.account_service.repository;

import com.vbank.account_service.entity.Account;
import com.vbank.account_service.entity.AccountStatus;
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

public interface AccountRepository extends JpaRepository<Account, UUID> {

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findAllByUserIdOrderByCreatedAtAsc(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from Account account where account.accountId = :accountId")
    Optional<Account> findByIdForUpdate(
            @Param("accountId") UUID accountId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Account account
               set account.status = :inactiveStatus,
                   account.updatedAt = :updatedAt
             where account.status = :activeStatus
               and account.lastTransactionAt is not null
               and account.lastTransactionAt < :cutoff
            """)
    int markStaleAccountsInactive(
            @Param("activeStatus") AccountStatus activeStatus,
            @Param("inactiveStatus") AccountStatus inactiveStatus,
            @Param("cutoff") Instant cutoff,
            @Param("updatedAt") Instant updatedAt
    );
}
