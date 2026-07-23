package com.vbank.bff_service.client;

import com.vbank.bff_service.dto.downstream.UserProfileDownstreamResponse;
import com.vbank.bff_service.exception.MalformedDownstreamResponseException;
import com.vbank.bff_service.model.RequestContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Component
public class UserServiceClient extends DownstreamClientSupport {

    public static final String SERVICE_NAME = "User Service";

    private final WebClient webClient;

    public UserServiceClient(
            @Qualifier("userWebClient") WebClient webClient,
            @Value("${bff.downstream.request-timeout}")
            Duration requestTimeout
    ) {
        super(requestTimeout);
        this.webClient = webClient;
    }

    public Mono<UserProfileDownstreamResponse> getProfile(
            UUID userId,
            RequestContext requestContext
    ) {
        return retrieve(
                SERVICE_NAME,
                webClient.get().uri(
                        "/users/{userId}/profile",
                        userId
                ),
                UserProfileDownstreamResponse.class,
                requestContext
        ).map(response -> validate(response, userId));
    }

    private UserProfileDownstreamResponse validate(
            UserProfileDownstreamResponse response,
            UUID requestedUserId
    ) {
        if (response.userId() == null
                || !response.userId().equals(requestedUserId)
                || isBlank(response.username())
                || isBlank(response.email())
                || isBlank(response.firstName())
                || isBlank(response.lastName())) {
            throw new MalformedDownstreamResponseException(
                    SERVICE_NAME,
                    "The profile is missing required fields or contains a mismatched user ID."
            );
        }

        return response;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
