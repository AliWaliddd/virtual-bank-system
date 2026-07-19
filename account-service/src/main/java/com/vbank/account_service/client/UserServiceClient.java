package com.vbank.account_service.client;

import com.vbank.account_service.exception.UserNotFoundException;
import com.vbank.account_service.exception.UserServiceAuthorizationException;
import com.vbank.account_service.exception.UserServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class UserServiceClient {

    private final RestClient restClient;
    private final String internalAuthorizationHeader;

    public UserServiceClient(
            @Value("${services.user.base-url}") String userServiceBaseUrl,
            @Value("${services.user.internal-authorization}")
            String internalAuthorizationHeader
    ) {
        this.restClient = RestClient.create(userServiceBaseUrl);
        this.internalAuthorizationHeader = internalAuthorizationHeader;
    }

    public void verifyUserExists(UUID userId) {
        try {
            restClient
                    .get()
                    .uri("/users/{userId}/profile", userId)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            internalAuthorizationHeader
                    )
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                throw new UserNotFoundException(
                        "User with ID " + userId + " not found."
                );
            }

            if (exception.getStatusCode().equals(HttpStatus.UNAUTHORIZED)
                    || exception.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
                throw new UserServiceAuthorizationException(
                        "User Service rejected the Account Service authorization."
                );
            }

            throw new UserServiceUnavailableException(
                    "User Service could not validate the requested user."
            );
        } catch (ResourceAccessException exception) {
            throw new UserServiceUnavailableException(
                    "User Service is currently unavailable."
            );
        }
    }
}
