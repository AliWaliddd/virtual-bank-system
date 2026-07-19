package com.vbank.account_service.scheduler;

import com.vbank.account_service.entity.AccountStatus;
import com.vbank.account_service.entity.AccountType;
import com.vbank.account_service.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class AccountInactivityScheduler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    AccountInactivityScheduler.class
            );

    private final AccountRepository accountRepository;
    private final Clock clock;
    private final Duration inactivityThreshold;

    public AccountInactivityScheduler(
            AccountRepository accountRepository,
            Clock clock,
            @Value(
                    "${account.inactivity.threshold-hours:24}"
            )
            long inactivityThresholdHours
    ) {
        if (inactivityThresholdHours <= 0) {
            throw new IllegalArgumentException(
                    "Account inactivity threshold must be greater than zero hours."
            );
        }

        this.accountRepository = accountRepository;
        this.clock = clock;
        this.inactivityThreshold =
                Duration.ofHours(inactivityThresholdHours);
    }

    @Scheduled(
            cron = "${account.inactivity.cron}",
            zone = "${account.inactivity.zone:UTC}"
    )
    @Transactional
    public void inactivateStaleAccounts() {
        Instant currentTime = clock.instant();

        Instant cutoff =
                currentTime.minus(inactivityThreshold);

        int inactiveAccountCount =
                accountRepository.markStaleAccountsInactive(
                        AccountStatus.ACTIVE,
                        AccountStatus.INACTIVE,
                        AccountType.SYSTEM,
                        cutoff,
                        currentTime
                );

        LOGGER.info(
                "Account inactivity job completed. "
                        + "Cutoff: {}. "
                        + "Accounts marked inactive: {}",
                cutoff,
                inactiveAccountCount
        );
    }
}