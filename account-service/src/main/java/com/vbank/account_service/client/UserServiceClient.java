package com.vbank.account_service.client;

import com.vbank.account_service.exception.UserNotFoundException;
import com.vbank.account_service.exception.UserServiceUnavailableException;
import com.vbank.account_service.model.RequestContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class UserServiceClient {

    private final RestClient restClient;
    private final String userServiceBaseUrl;

    public UserServiceClient(
            RestClient restClient,
            @Value("${services.user.base-url}")
            String userServiceBaseUrl

    ) {
        this.restClient = restClient;
        this.userServiceBaseUrl = userServiceBaseUrl;
    }

    public void verifyUserExists(
            UUID userId,
            RequestContext requestContext
    ) {
        try {
            restClient
                    .get()
                    .uri(
                            userServiceBaseUrl
                                    + "/users/{userId}/profile",
                            userId
                    )
                    .headers(requestContext::applyTo)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                throw new UserNotFoundException(
                        "User with ID " + userId + " not found."
                );
            }

            throw new UserServiceUnavailableException(
                    "User Service could not validate the requested user."
            );
        } catch (ResourceAccessException exception) {
            throw new UserServiceUnavailableException(
                    "User Service is currently unavailable."
            );
        } catch (RestClientException exception) {
            throw new UserServiceUnavailableException(
                    "An invalid response was received from User Service."
            );
        }
    }
}
