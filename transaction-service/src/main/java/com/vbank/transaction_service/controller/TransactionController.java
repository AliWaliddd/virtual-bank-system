package com.vbank.transaction_service.controller;

import com.vbank.transaction_service.dto.TransactionResponse;
import com.vbank.transaction_service.dto.TransferInitiationRequest;
import com.vbank.transaction_service.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer/initiation")
    public ResponseEntity<TransactionResponse> initiateTransfer(
            @Valid @RequestBody TransferInitiationRequest request) {

        TransactionResponse response =
                transactionService.initiateTransfer(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @PathVariable UUID accountId) {

        List<TransactionResponse> transactions =
                transactionService.getTransactions(accountId);

        return ResponseEntity.ok(transactions);
    }
}