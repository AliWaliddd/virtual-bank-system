package com.vbank.bff_service.service;

import com.vbank.bff_service.client.AccountServiceClient;
import com.vbank.bff_service.client.TransactionServiceClient;
import com.vbank.bff_service.client.UserServiceClient;
import com.vbank.bff_service.dto.downstream.AccountDownstreamResponse;
import com.vbank.bff_service.dto.downstream.TransactionDownstreamResponse;
import com.vbank.bff_service.dto.downstream.UserProfileDownstreamResponse;
import com.vbank.bff_service.dto.response.DashboardAccountResponse;
import com.vbank.bff_service.dto.response.DashboardResponse;
import com.vbank.bff_service.dto.response.DashboardTransactionResponse;
import com.vbank.bff_service.model.RequestContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
public class DashboardService {

    private final UserServiceClient userServiceClient;
    private final AccountServiceClient accountServiceClient;
    private final TransactionServiceClient transactionServiceClient;
    private final int transactionConcurrency;

    public DashboardService(
            UserServiceClient userServiceClient,
            AccountServiceClient accountServiceClient,
            TransactionServiceClient transactionServiceClient,
            @Value("${bff.downstream.transaction-concurrency:8}")
            int transactionConcurrency
    ) {
        if (transactionConcurrency < 1) {
            throw new IllegalArgumentException(
                    "Transaction concurrency must be at least 1."
            );
        }

        this.userServiceClient = userServiceClient;
        this.accountServiceClient = accountServiceClient;
        this.transactionServiceClient = transactionServiceClient;
        this.transactionConcurrency = transactionConcurrency;
    }

    public Mono<DashboardResponse> getDashboard(
            UUID userId,
            RequestContext requestContext
    ) {
        Mono<UserProfileDownstreamResponse> profileMono =
                userServiceClient.getProfile(
                        userId,
                        requestContext
                );

        Mono<List<AccountDownstreamResponse>> accountsMono =
                accountServiceClient.getAccounts(
                        userId,
                        requestContext
                );

        return Mono.zip(profileMono, accountsMono)
                .flatMap(tuple -> enrichAccounts(
                        tuple.getT2(),
                        requestContext
                ).map(accounts -> toDashboardResponse(
                        tuple.getT1(),
                        accounts
                )));
    }

    private Mono<List<DashboardAccountResponse>> enrichAccounts(
            List<AccountDownstreamResponse> accounts,
            RequestContext requestContext
    ) {
        if (accounts.isEmpty()) {
            return Mono.just(List.of());
        }

        return Flux.fromIterable(accounts)
                .flatMapSequential(
                        account -> transactionServiceClient
                                .getTransactions(
                                        account.accountId(),
                                        requestContext
                                )
                                .map(transactions -> toDashboardAccount(
                                        account,
                                        transactions
                                )),
                        transactionConcurrency,
                        1
                )
                .collectList();
    }

    private DashboardResponse toDashboardResponse(
            UserProfileDownstreamResponse profile,
            List<DashboardAccountResponse> accounts
    ) {
        return new DashboardResponse(
                profile.userId(),
                profile.username(),
                profile.email(),
                profile.firstName(),
                profile.lastName(),
                accounts
        );
    }

    private DashboardAccountResponse toDashboardAccount(
            AccountDownstreamResponse account,
            List<TransactionDownstreamResponse> transactions
    ) {
        List<DashboardTransactionResponse> transactionResponses =
                transactions.stream()
                        .map(this::toDashboardTransaction)
                        .toList();

        return new DashboardAccountResponse(
                account.accountId(),
                account.accountNumber(),
                account.accountType(),
                account.balance(),
                account.status(),
                transactionResponses
        );
    }

    private DashboardTransactionResponse toDashboardTransaction(
            TransactionDownstreamResponse transaction
    ) {
        return new DashboardTransactionResponse(
                transaction.transactionId(),
                transaction.fromAccountId(),
                transaction.toAccountId(),
                transaction.amount(),
                transaction.status(),
                transaction.failureReason(),
                transaction.createdAt(),
                transaction.executedAt()
        );
    }
}
