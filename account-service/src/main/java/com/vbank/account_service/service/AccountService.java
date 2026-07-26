package com.vbank.account_service.service;

import com.vbank.account_service.client.UserServiceClient;
import com.vbank.account_service.dto.LogMessage;
import com.vbank.account_service.dto.MessageType;
import com.vbank.account_service.dto.request.CreateAccountRequest;
import com.vbank.account_service.dto.request.TransferRequest;
import com.vbank.account_service.dto.response.AccountResponse;
import com.vbank.account_service.dto.response.ActivateAccountResponse;
import com.vbank.account_service.dto.response.CreateAccountResponse;
import com.vbank.account_service.dto.response.TransferResponse;
import com.vbank.account_service.entity.Account;
import com.vbank.account_service.entity.AccountStatus;
import com.vbank.account_service.entity.AccountType;
import com.vbank.account_service.exception.AccountNotFoundException;
import com.vbank.account_service.exception.BalanceLimitExceededException;
import com.vbank.account_service.exception.InsufficientFundsException;
import com.vbank.account_service.exception.InvalidAccountOperationException;
import com.vbank.account_service.exception.NoAccountsFoundException;
import com.vbank.account_service.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class AccountService {

    private static final BigDecimal MAX_BALANCE =
            new BigDecimal("99999999999999999.99");

    private final AccountRepository accountRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final UserServiceClient userServiceClient;
    private final Clock clock;
    private final LoggingProducerService loggingProducerService;

    public AccountService(
            AccountRepository accountRepository,
            AccountNumberGenerator accountNumberGenerator,
            UserServiceClient userServiceClient,
            Clock clock,
            LoggingProducerService loggingProducerService
    ) {
        this.accountRepository = accountRepository;
        this.accountNumberGenerator = accountNumberGenerator;
        this.userServiceClient = userServiceClient;
        this.clock = clock;
        this.loggingProducerService = loggingProducerService;
    }

    public CreateAccountResponse createAccount(
            CreateAccountRequest request
    ) {
        userServiceClient.verifyUserExists(request.userId());

        AccountType accountType = parsePublicAccountType(
                request.accountType()
        );

        BigDecimal initialBalance = normalizeMoney(
                request.initialBalance()
        );

        if (initialBalance.compareTo(MAX_BALANCE) > 0) {
            logOperation(
                    "Initial balance exceeds the permitted account balance limit.",
                    "POST",
                    "/accounts",
                    400,
                    request.userId()
            );

            throw new BalanceLimitExceededException(
                    "Initial balance exceeds the permitted account balance limit."
            );
        }

        String accountNumber =
                accountNumberGenerator.generateUniqueAccountNumber();

        Account account = new Account(
                request.userId(),
                accountNumber,
                accountType,
                initialBalance
        );

        Account savedAccount = accountRepository.save(account);
        logOperation(
                "Account created successfully.",
                "POST",
                "/accounts",
                201,
                savedAccount.getAccountId()
        );

        return new CreateAccountResponse(
                savedAccount.getAccountId(),
                savedAccount.getAccountNumber(),
                "Account created successfully."
        );
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID accountId) {
        Account account = findAccount(accountId);
        logOperation(
                "Account retrieved successfully.",
                "GET",
                "/accounts/{accountId}",
                200,
                account.getAccountId()
        );

        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUser(UUID userId) {
        List<Account> accounts =
                accountRepository.findAllByUserIdOrderByCreatedAtAsc(
                        userId
                );

        if (accounts.isEmpty()) {
            throw new NoAccountsFoundException(
                    "No accounts found for user ID " + userId + "."
            );
        }

        logOperation(
                "Accounts retrieved successfully for user.",
                "GET",
                "/users/{userId}/accounts",
                200,
                userId
        );

        return accounts.stream()
                .map(this::toResponse)
                .toList();
    }

    public ActivateAccountResponse activateAccount(
            UUID accountId
    ) {
        Account account = findAccountForUpdate(accountId);

        if (account.getAccountType() == AccountType.SYSTEM) {
            throw new InvalidAccountOperationException(
                    "SYSTEM accounts cannot be activated through the public account endpoint."
            );
        }

        /*
         * The operation is idempotent.
         *
         * Calling activate on an already-active account does not
         * alter reactivatedAt or extend its inactivity period.
         */
        if (account.getStatus() == AccountStatus.ACTIVE) {
            logOperation(
                    "Account activation skipped because account is already active.",
                    "PUT",
                    "/accounts/{accountId}/activate",
                    200,
                    account.getAccountId()
            );

            return new ActivateAccountResponse(
                    account.getAccountId(),
                    account.getStatus(),
                    account.getReactivatedAt(),
                    "Account is already active."
            );
        }

        Instant activationTime = clock.instant();

        account.activate(activationTime);

        logOperation(
                "Account activated successfully.",
                "PUT",
                "/accounts/{accountId}/activate",
                200,
                account.getAccountId()
        );

        return new ActivateAccountResponse(
                account.getAccountId(),
                account.getStatus(),
                account.getReactivatedAt(),
                "Account activated successfully."
        );
    }

    public TransferResponse transfer(TransferRequest request) {
        validateTransferIdentifiers(request);

        if (request.fromAccountId().equals(
                request.toAccountId()
        )) {
            logOperation(
                    "Transfer rejected because source and destination accounts are the same.",
                    "PUT",
                    "/accounts/transfer",
                    400,
                    request.fromAccountId()
            );

            throw new InvalidAccountOperationException(
                    "The source and destination accounts must be different."
            );
        }

        BigDecimal amount = normalizeMoney(request.amount());

        if (amount.signum() <= 0) {
            logOperation(
                    "Transfer rejected because amount is not greater than zero.",
                    "PUT",
                    "/accounts/transfer",
                    400,
                    request.fromAccountId()
            );

            throw new InvalidAccountOperationException(
                    "Transfer amount must be greater than zero."
            );
        }

        LockedAccounts lockedAccounts = lockAccountsInStableOrder(
                request.fromAccountId(),
                request.toAccountId()
        );

        Account fromAccount = lockedAccounts.fromAccount();
        Account toAccount = lockedAccounts.toAccount();

        validateActive(fromAccount);
        validateActive(toAccount);

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Account "
                            + fromAccount.getAccountId()
                            + " has insufficient funds."
            );
        }

        BigDecimal destinationBalance =
                toAccount.getBalance().add(amount);

        if (destinationBalance.compareTo(MAX_BALANCE) > 0) {
            throw new BalanceLimitExceededException(
                    "The transfer would exceed the destination account balance limit."
            );
        }

        Instant transactionTime = clock.instant();

        fromAccount.debit(amount, transactionTime);
        toAccount.credit(amount, transactionTime);

        logOperation(
                "Transfer completed successfully from account "
                        + fromAccount.getAccountId()
                        + " to account "
                        + toAccount.getAccountId()
                        + ".",
                "PUT",
                "/accounts/transfer",
                200,
                fromAccount.getAccountId()
        );

        return new TransferResponse(
                "Account updated successfully."
        );
    }

    private void validateTransferIdentifiers(
            TransferRequest request
    ) {
        if (request.fromAccountId() == null) {
            throw new InvalidAccountOperationException(
                    "Source account ID is required."
            );
        }

        if (request.toAccountId() == null) {
            throw new InvalidAccountOperationException(
                    "Destination account ID is required."
            );
        }
    }

    private LockedAccounts lockAccountsInStableOrder(
            UUID fromAccountId,
            UUID toAccountId
    ) {
        UUID firstId;
        UUID secondId;

        if (fromAccountId.compareTo(toAccountId) < 0) {
            firstId = fromAccountId;
            secondId = toAccountId;
        } else {
            firstId = toAccountId;
            secondId = fromAccountId;
        }

        Account firstAccount = findAccountForUpdate(firstId);
        Account secondAccount = findAccountForUpdate(secondId);

        if (firstAccount.getAccountId().equals(
                fromAccountId
        )) {
            return new LockedAccounts(
                    firstAccount,
                    secondAccount
            );
        }

        return new LockedAccounts(
                secondAccount,
                firstAccount
        );
    }

    private Account findAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(
                        () -> accountNotFound(accountId)
                );
    }

    private Account findAccountForUpdate(UUID accountId) {
        return accountRepository
                .findByIdForUpdate(accountId)
                .orElseThrow(
                        () -> accountNotFound(accountId)
                );
    }

    private AccountNotFoundException accountNotFound(
            UUID accountId
    ) {
        return new AccountNotFoundException(
                "Account with ID "
                        + accountId
                        + " not found."
        );
    }

    private void validateActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountOperationException(
                    "Account "
                            + account.getAccountId()
                            + " is inactive and cannot participate in a transfer."
            );
        }
    }

    private AccountType parsePublicAccountType(
            String value
    ) {
        if (value == null || value.isBlank()) {
            throw new InvalidAccountOperationException(
                    "Account type is required."
            );
        }

        AccountType accountType;

        try {
            accountType = AccountType.valueOf(
                    value.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new InvalidAccountOperationException(
                    "Account type must be SAVINGS or CHECKING."
            );
        }

        if (accountType == AccountType.SYSTEM) {
            throw new InvalidAccountOperationException(
                    "SYSTEM accounts cannot be created through the public account endpoint."
            );
        }

        return accountType;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            throw new InvalidAccountOperationException(
                    "Monetary value is required."
            );
        }

        try {
            return value.setScale(2);
        } catch (ArithmeticException exception) {
            throw new InvalidAccountOperationException(
                    "Monetary values may contain at most two decimal places."
            );
        }
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getAccountId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getStatus()
        );
    }

    private void logOperation(
            String message,
            String httpMethod,
            String path,
            Integer statusCode,
            UUID correlationId
    ) {
        loggingProducerService.send(
                LogMessage.builder()
                        .message(message)
                        .messageType(MessageType.REQUEST)
                        .dateTime(clock.instant())
                        .serviceName("account-service")
                        .httpMethod(httpMethod)
                        .path(path)
                        .statusCode(statusCode)
                        .correlationId(correlationId)
                        .appName("Virtual Bank")
                        .build()
        );
    }

    private record LockedAccounts(
            Account fromAccount,
            Account toAccount
    ) {
    }
}
