package com.vbank.bff_service.client;

import com.vbank.bff_service.dto.downstream.AccountDownstreamResponse;
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
public class AccountServiceClient extends DownstreamClientSupport {

    public static final String SERVICE_NAME = "Account Service";

    private static final ParameterizedTypeReference<List<AccountDownstreamResponse>>
            ACCOUNT_LIST_TYPE = new ParameterizedTypeReference<>() {
            };

    private final WebClient webClient;

    public AccountServiceClient(
            @Qualifier("accountWebClient") WebClient webClient,
            @Value("${bff.downstream.request-timeout}")
            Duration requestTimeout
    ) {
        super(requestTimeout);
        this.webClient = webClient;
    }

    public Mono<List<AccountDownstreamResponse>> getAccounts(
            UUID userId,
            RequestContext requestContext
    ) {
        return retrieve(
                SERVICE_NAME,
                webClient.get().uri(
                        "/users/{userId}/accounts",
                        userId
                ),
                ACCOUNT_LIST_TYPE,
                requestContext
        )
                .map(this::validate)
                .onErrorResume(
                        DownstreamHttpException.class,
                        exception -> isNoAccountsResponse(exception)
                                ? Mono.just(List.of())
                                : Mono.error(exception)
                );
    }

    private List<AccountDownstreamResponse> validate(
            List<AccountDownstreamResponse> accounts
    ) {
        if (accounts == null) {
            throw malformed("The account list was null.");
        }

        Set<UUID> accountIds = new HashSet<>();

        for (AccountDownstreamResponse account : accounts) {
            if (account == null
                    || account.accountId() == null
                    || isBlank(account.accountNumber())
                    || isBlank(account.accountType())
                    || account.balance() == null
                    || isBlank(account.status())) {
                throw malformed(
                        "At least one account is missing required fields."
                );
            }

            if (!accountIds.add(account.accountId())) {
                throw malformed(
                        "The response contains a duplicate account ID."
                );
            }
        }

        return List.copyOf(accounts);
    }

    private boolean isNoAccountsResponse(
            DownstreamHttpException exception
    ) {
        String message = exception.getDownstreamMessage();

        return exception.getDownstreamStatus() == 404
                && message != null
                && message.toLowerCase()
                .contains("no accounts found");
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
