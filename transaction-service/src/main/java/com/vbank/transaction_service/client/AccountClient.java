package com.vbank.transaction_service.client;

import com.vbank.transaction_service.dto.Request.TransferRequest;
import com.vbank.transaction_service.dto.Response.AccountResponse;
import com.vbank.transaction_service.dto.Response.TransferResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
                .uri(accountServiceUrl + "/accounts/{accountId}", accountId)
                .retrieve()
                .body(AccountResponse.class);
    }

    public TransferResponse  transfer(TransferRequest request){
        return restClient.put()
                .uri(accountServiceUrl+"/accounts/transfer")
                .body(request)
                .retrieve()
                .body(TransferResponse.class);
    }
}