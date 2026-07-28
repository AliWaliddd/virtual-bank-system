package com.vbank.bff_service.controller;

import com.vbank.bff_service.dto.response.DashboardResponse;
import com.vbank.bff_service.exception.InvalidUuidException;
import com.vbank.bff_service.filter.CorrelationIdFilter;
import com.vbank.bff_service.model.RequestContext;
import com.vbank.bff_service.service.DashboardService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping(
            value = "/bff/dashboard/{userId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<DashboardResponse> getDashboard(
            @PathVariable String userId,
            @RequestHeader(
                    value = "APP-NAME",
                    required = false
            ) String appName,
            @RequestHeader(CorrelationIdFilter.HEADER_NAME)
            String correlationId
    ) {

        UUID parsedUserId = parseUserId(userId);

        RequestContext requestContext = new RequestContext(
                appName,
                correlationId
        );

        return dashboardService.getDashboard(
                parsedUserId,
                requestContext
        );
    }

    private UUID parseUserId(String userId) {
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException exception) {
            throw new InvalidUuidException(
                    "Invalid value for 'userId'."
            );
        }
    }
}
