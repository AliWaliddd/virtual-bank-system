package com.vbank.account_service.service;

import com.vbank.account_service.dto.LogMessage;
import com.vbank.account_service.dto.MessageType;
import com.vbank.account_service.entity.AccountStatus;
import com.vbank.account_service.entity.AccountType;
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
    private final LoggingProducerService loggingProducerService;

    public AccountInactivityService(
            AccountRepository accountRepository,
            Clock clock,
            LoggingProducerService loggingProducerService
    ) {
        this.accountRepository = accountRepository;
        this.clock = clock;
        this.loggingProducerService = loggingProducerService;
    }

    @Transactional
    public int inactivateStaleAccounts() {
        Instant now = clock.instant();
        Instant cutoff = now.minus(INACTIVITY_PERIOD);

        int inactiveAccountCount = accountRepository.markStaleAccountsInactive(
                AccountStatus.ACTIVE,
                AccountStatus.INACTIVE,
                AccountType.SYSTEM,
                cutoff,
                now
        );

        loggingProducerService.send(
                LogMessage.builder()
                        .message(
                                "Account inactivity operation completed. Accounts marked inactive: "
                                        + inactiveAccountCount
                                        + "."
                        )
                        .messageType(MessageType.REQUEST)
                        .dateTime(now)
                        .serviceName("account-service")
                        .httpMethod("SCHEDULED")
                        .path("/accounts/inactivity")
                        .statusCode(200)
                        .appName("Virtual Bank")
                        .build()
        );

        return inactiveAccountCount;
    }
}
