package com.vbank.bff_service.client;

import com.vbank.bff_service.dto.downstream.TransactionDownstreamResponse;
import com.vbank.bff_service.exception.DownstreamHttpException;
import com.vbank.bff_service.exception.MalformedDownstreamResponseException;
import com.vbank.bff_service.model.RequestContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class TransactionServiceClient
        extends DownstreamClientSupport {

    public static final String SERVICE_NAME = "Transaction Service";

    private static final ParameterizedTypeReference<List<TransactionDownstreamResponse>>
            TRANSACTION_LIST_TYPE = new ParameterizedTypeReference<>() {
            };

    private final WebClient webClient;

    public TransactionServiceClient(
            @Qualifier("transactionWebClient") WebClient webClient,
            @Value("${bff.downstream.request-timeout}")
            Duration requestTimeout
    ) {
        super(requestTimeout);
        this.webClient = webClient;
    }

    public Mono<List<TransactionDownstreamResponse>> getTransactions(
            UUID accountId,
            RequestContext requestContext
    ) {
        return retrieve(
                SERVICE_NAME,
                webClient.get().uri(
                        "/accounts/{accountId}/transactions",
                        accountId
                ),
                TRANSACTION_LIST_TYPE,
                requestContext
        )
                .map(transactions -> validate(
                        transactions,
                        accountId
                ))
                .onErrorResume(
                        DownstreamHttpException.class,
                        exception -> isNoTransactionsResponse(exception)
                                ? Mono.just(List.of())
                                : Mono.error(exception)
                );
    }

    private List<TransactionDownstreamResponse> validate(
            List<TransactionDownstreamResponse> transactions,
            UUID requestedAccountId
    ) {
        if (transactions == null) {
            throw malformed("The transaction list was null.");
        }

        Set<UUID> transactionIds = new HashSet<>();

        for (TransactionDownstreamResponse transaction : transactions) {
            if (transaction == null
                    || transaction.transactionId() == null
                    || transaction.fromAccountId() == null
                    || transaction.toAccountId() == null
                    || transaction.amount() == null
                    || isBlank(transaction.status())
                    || transaction.createdAt() == null) {
                throw malformed(
                        "At least one transaction is missing required fields."
                );
            }

            boolean referencesAccount = requestedAccountId.equals(
                    transaction.fromAccountId()
            ) || requestedAccountId.equals(
                    transaction.toAccountId()
            );

            if (!referencesAccount) {
                throw malformed(
                        "A returned transaction does not reference the requested account."
                );
            }

            if (!transactionIds.add(transaction.transactionId())) {
                throw malformed(
                        "The response contains a duplicate transaction ID."
                );
            }
        }

        return List.copyOf(transactions);
    }

    private boolean isNoTransactionsResponse(
            DownstreamHttpException exception
    ) {
        String message = exception.getDownstreamMessage();

        return exception.getDownstreamStatus() == 404
                && message != null
                && message.toLowerCase()
                .contains("no transactions found");
    }

    private MalformedDownstreamResponseException malformed(
            String detail
    ) {
        return new MalformedDownstreamResponseException(
                SERVICE_NAME,
                detail
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
