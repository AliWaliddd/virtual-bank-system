package com.vbank.account_service.scheduler;

import com.vbank.account_service.service.AccountInactivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AccountInactivityScheduler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AccountInactivityScheduler.class);

    private final AccountInactivityService accountInactivityService;

    public AccountInactivityScheduler(
            AccountInactivityService accountInactivityService
    ) {
        this.accountInactivityService = accountInactivityService;
    }

    @Scheduled(
            cron = "${account.inactivity.cron:0 0 * * * *}",
            zone = "${account.inactivity.zone:UTC}"
    )
    public void inactivateStaleAccounts() {
        int updatedAccounts =
                accountInactivityService.inactivateStaleAccounts();

        LOGGER.info(
                "Account inactivity job completed. Accounts marked inactive: {}",
                updatedAccounts
        );
    }
}
