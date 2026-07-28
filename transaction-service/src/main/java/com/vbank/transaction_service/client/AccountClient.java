package com.vbank.transaction_service.client;

import com.vbank.transaction_service.dto.Request.TransferRequest;
import com.vbank.transaction_service.dto.Response.AccountResponse;
import com.vbank.transaction_service.dto.Response.DownstreamErrorResponse;
import com.vbank.transaction_service.dto.Response.TransferResponse;
import com.vbank.transaction_service.exceptions.AccountServiceBadGatewayException;
import com.vbank.transaction_service.exceptions.AccountServiceUnavailableException;
import com.vbank.transaction_service.exceptions.AccountTransferConflictException;
import com.vbank.transaction_service.exceptions.AccountTransferRejectedException;
import com.vbank.transaction_service.model.RequestContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class AccountClient {

    private final RestClient restClient;
    private final String accountServiceUrl;

    public AccountClient(
            RestClient restClient,
            @Value("${account.service.url}")
            String accountServiceUrl
    ) {
        this.restClient = restClient;
        this.accountServiceUrl = accountServiceUrl;
    }

    public AccountResponse getAccount(
            UUID accountId,
            RequestContext requestContext
    ) {
        try {
            AccountResponse response = restClient.get()
                    .uri(
                            accountServiceUrl
                                    + "/accounts/{accountId}",
                            accountId
                    )
                    .headers(requestContext::applyTo)
                    .retrieve()
                    .body(AccountResponse.class);

            if (response == null) {
                throw new AccountServiceBadGatewayException(
                        "Account Service returned an empty account response."
                );
            }

            return response;
        } catch (HttpClientErrorException.NotFound exception) {
            throw exception;
        } catch (HttpClientErrorException exception) {
            throw new AccountServiceBadGatewayException(
                    "Account Service rejected the internal account lookup unexpectedly."
            );
        } catch (HttpServerErrorException exception) {
            throw new AccountServiceBadGatewayException(
                    "Account Service failed while retrieving the account."
            );
        } catch (ResourceAccessException exception) {
            throw new AccountServiceUnavailableException(
                    "Account Service is currently unavailable."
            );
        } catch (RestClientException exception) {
            throw new AccountServiceBadGatewayException(
                    "An invalid account response was received from Account Service."
            );
        }
    }

    public TransferResponse transfer(
            TransferRequest request,
            RequestContext requestContext
    ) {
        try {
            TransferResponse response = restClient.put()
                    .uri(accountServiceUrl + "/accounts/transfer")
                    .headers(requestContext::applyTo)
                    .body(request)
                    .retrieve()
                    .body(TransferResponse.class);

            if (response == null) {
                throw new AccountServiceBadGatewayException(
                        "Account Service returned an empty transfer response."
                );
            }

            return response;
        } catch (
                HttpClientErrorException.BadRequest
                | HttpClientErrorException.NotFound exception
        ) {
            throw new AccountTransferRejectedException(
                    extractDownstreamMessage(
                            exception,
                            "Account Service rejected the transfer."
                    )
            );
        } catch (HttpClientErrorException.Conflict exception) {
            throw new AccountTransferConflictException(
                    extractDownstreamMessage(
                            exception,
                            "The accounts are currently being updated. Please retry the transfer."
                    )
            );
        } catch (HttpClientErrorException exception) {
            throw new AccountServiceBadGatewayException(
                    "Account Service rejected the internal request unexpectedly."
            );
        } catch (HttpServerErrorException exception) {
            throw new AccountServiceBadGatewayException(
                    "Account Service failed while processing the transfer."
            );
        } catch (ResourceAccessException exception) {
            throw new AccountServiceUnavailableException(
                    "Account Service is currently unavailable."
            );
        } catch (RestClientException exception) {
            throw new AccountServiceBadGatewayException(
                    "An invalid response was received from Account Service."
            );
        }
    }

    private String extractDownstreamMessage(
            RestClientResponseException exception,
            String fallbackMessage
    ) {
        try {
            DownstreamErrorResponse errorResponse =
                    exception.getResponseBodyAs(
                            DownstreamErrorResponse.class
                    );

            if (errorResponse != null
                    && errorResponse.message() != null
                    && !errorResponse.message().isBlank()) {
                return errorResponse.message();
            }
        } catch (RuntimeException ignored) {
            // Use the safe fallback below.
        }

        return fallbackMessage;
    }
}
