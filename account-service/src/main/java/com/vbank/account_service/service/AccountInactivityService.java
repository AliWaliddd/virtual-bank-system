package com.vbank.account_service.service;

import com.vbank.account_service.dto.LogMessage;
import com.vbank.account_service.dto.MessageType;
import com.vbank.account_service.entity.AccountStatus;
import com.vbank.account_service.entity.AccountType;
import com.vbank.account_service.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AccountInactivityService {

    private final AccountRepository accountRepository;
    private final Clock clock;
    private final LoggingProducerService loggingProducerService;
    private final Duration inactivityThreshold;

    public AccountInactivityService(
            AccountRepository accountRepository,
            Clock clock,
            LoggingProducerService loggingProducerService,
            @Value("${account.inactivity.threshold-hours:24}")
            long inactivityThresholdHours
    ) {
        if (inactivityThresholdHours <= 0) {
            throw new IllegalArgumentException(
                    "Account inactivity threshold must be greater than zero hours."
            );
        }

        this.accountRepository = accountRepository;
        this.clock = clock;
        this.loggingProducerService = loggingProducerService;
        this.inactivityThreshold =
                Duration.ofHours(inactivityThresholdHours);
    }

    @Transactional
    public int inactivateStaleAccounts() {
        Instant currentTime = clock.instant();
        Instant cutoff = currentTime.minus(inactivityThreshold);

        int inactiveAccountCount =
                accountRepository.markStaleAccountsInactive(
                        AccountStatus.ACTIVE,
                        AccountStatus.INACTIVE,
                        AccountType.SYSTEM,
                        cutoff,
                        currentTime
                );

        loggingProducerService.send(
                LogMessage.builder()
                        .message(
                                "Account inactivity job completed. Accounts marked inactive: "
                                        + inactiveAccountCount
                                        + "."
                        )
                        .messageType(MessageType.RESPONSE)
                        .dateTime(currentTime)
                        .serviceName("account-service")
                        .httpMethod("SCHEDULED")
                        .path("/internal/accounts/inactivity")
                        .statusCode(200)
                        .correlationId(UUID.randomUUID())
                        .appName("SYSTEM")
                        .build()
        );

        return inactiveAccountCount;
    }
}
