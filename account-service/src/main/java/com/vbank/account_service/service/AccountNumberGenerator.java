package com.vbank.account_service.service;

import com.vbank.account_service.exception.AccountNumberGenerationException;
import com.vbank.account_service.repository.AccountRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AccountNumberGenerator {

    private static final int ACCOUNT_NUMBER_LENGTH = 10;
    private static final int MAX_GENERATION_ATTEMPTS = 20;

    private final AccountRepository accountRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountNumberGenerator(
            AccountRepository accountRepository
    ) {
        this.accountRepository = accountRepository;
    }

    public String generateUniqueAccountNumber() {
        for (int attempt = 0;
             attempt < MAX_GENERATION_ATTEMPTS;
             attempt++) {

            String accountNumber = generateCandidate();

            if (!accountRepository.existsByAccountNumber(accountNumber)) {
                return accountNumber;
            }
        }

        throw new AccountNumberGenerationException(
                "A unique account number could not be generated."
        );
    }

    private String generateCandidate() {
        StringBuilder candidate =
                new StringBuilder(ACCOUNT_NUMBER_LENGTH);

        for (int index = 0;
             index < ACCOUNT_NUMBER_LENGTH;
             index++) {
            candidate.append(secureRandom.nextInt(10));
        }

        return candidate.toString();
    }
}
