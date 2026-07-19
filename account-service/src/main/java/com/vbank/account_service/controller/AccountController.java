package com.vbank.account_service.controller;

import com.vbank.account_service.dto.request.CreateAccountRequest;
import com.vbank.account_service.dto.request.TransferRequest;
import com.vbank.account_service.dto.response.AccountResponse;
import com.vbank.account_service.dto.response.CreateAccountResponse;
import com.vbank.account_service.dto.response.TransferResponse;
import com.vbank.account_service.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/accounts")
    public ResponseEntity<CreateAccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request
    ) {
        CreateAccountResponse response =
                accountService.createAccount(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /*
     * Excludes the reserved literal path "/accounts/transfer".
     *
     * Other invalid values such as "/accounts/not-a-uuid" still reach
     * UUID conversion and are handled as 400 Bad Request.
     */
    @GetMapping("/accounts/{accountId:(?!transfer$).+}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable("accountId") UUID accountId
    ) {
        return ResponseEntity.ok(
                accountService.getAccount(accountId)
        );
    }

    @GetMapping("/users/{userId}/accounts")
    public ResponseEntity<List<AccountResponse>> getAccountsByUser(
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(
                accountService.getAccountsByUser(userId)
        );
    }

    @PutMapping("/accounts/transfer")
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request
    ) {
        return ResponseEntity.ok(
                accountService.transfer(request)
        );
    }
}