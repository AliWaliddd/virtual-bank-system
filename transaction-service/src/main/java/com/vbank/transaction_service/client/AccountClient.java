package com.vbank.transaction_service.client;

import com.vbank.transaction_service.dto.Request.TransferRequest;
import com.vbank.transaction_service.dto.Response.AccountResponse;
import com.vbank.transaction_service.dto.Response.DownstreamErrorResponse;
import com.vbank.transaction_service.dto.Response.TransferResponse;
import com.vbank.transaction_service.exceptions.AccountServiceBadGatewayException;
import com.vbank.transaction_service.exceptions.AccountServiceUnavailableException;
import com.vbank.transaction_service.exceptions.AccountTransferConflictException;
import com.vbank.transaction_service.exceptions.AccountTransferRejectedException;
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

    @Value("${account.service.url}")
    private String accountServiceUrl;

    public AccountClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public AccountResponse getAccount(UUID accountId) {
        return restClient.get()
                .uri(
                        accountServiceUrl
                                + "/accounts/{accountId}",
                        accountId
                )
                .retrieve()
                .body(AccountResponse.class);
    }

    public TransferResponse transfer(TransferRequest request) {
        try {
            TransferResponse response = restClient.put()
                    .uri(accountServiceUrl + "/accounts/transfer")
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
            /*
             * Business rejection:
             * - insufficient funds
             * - inactive account
             * - account not found
             * - invalid transfer
             * - destination balance limit
             */
            throw new AccountTransferRejectedException(
                    extractDownstreamMessage(
                            exception,
                            "Account Service rejected the transfer."
                    )
            );

        } catch (HttpClientErrorException.Conflict exception) {
            /*
             * Usually a temporary concurrent-update or locking
             * conflict. The caller may retry execution.
             */
            throw new AccountTransferConflictException(
                    extractDownstreamMessage(
                            exception,
                            "The accounts are currently being updated. Please retry the transfer."
                    )
            );

        } catch (HttpClientErrorException exception) {
            /*
             * Unexpected downstream 4xx statuses, such as internal
             * authorization or routing problems, should not be
             * presented as an ordinary banking validation error.
             */
            throw new AccountServiceBadGatewayException(
                    "Account Service rejected the internal request unexpectedly."
            );

        } catch (HttpServerErrorException exception) {
            throw new AccountServiceBadGatewayException(
                    "Account Service failed while processing the transfer."
            );

        } catch (ResourceAccessException exception) {
            /*
             * Covers connection failures and, depending on the
             * configured HTTP client, network timeouts.
             */
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
            /*
             * The downstream body may be empty, malformed, or use
             * an unexpected structure. Use the safe fallback.
             */
        }

        return fallbackMessage;
    }
}