package com.vbank.account_service.service;

import com.vbank.account_service.entity.AccountStatus;
import com.vbank.account_service.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class AccountInactivityService {

    private static final Duration INACTIVITY_PERIOD =
            Duration.ofHours(24);

    private final AccountRepository accountRepository;
    private final Clock clock;

    public AccountInactivityService(
            AccountRepository accountRepository,
            Clock clock
    ) {
        this.accountRepository = accountRepository;
        this.clock = clock;
    }

    @Transactional
    public int inactivateStaleAccounts() {
        Instant now = clock.instant();
        Instant cutoff = now.minus(INACTIVITY_PERIOD);

        return accountRepository.markStaleAccountsInactive(
                AccountStatus.ACTIVE,
                AccountStatus.INACTIVE,
                cutoff,
                now
        );
    }
}
