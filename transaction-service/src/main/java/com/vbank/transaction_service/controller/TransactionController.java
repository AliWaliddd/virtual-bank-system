package com.vbank.transaction_service.controller;

import com.vbank.transaction_service.dto.TransactionResponse;
import com.vbank.transaction_service.dto.TransferExecutionRequest;
import com.vbank.transaction_service.dto.TransferInitiationRequest;
import com.vbank.transaction_service.model.RequestContext;
import com.vbank.transaction_service.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transactions/transfer/initiation")
    public ResponseEntity<TransactionResponse> initiateTransfer(
            @Valid @RequestBody TransferInitiationRequest request,
            HttpServletRequest servletRequest
    ) {
        TransactionResponse response =
                transactionService.initiateTransfer(
                        request,
                        RequestContext.from(servletRequest)
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/transactions/transfer/execution")
    public ResponseEntity<TransactionResponse> executeTransfer(
            @Valid @RequestBody TransferExecutionRequest request,
            HttpServletRequest servletRequest
    ) {
        TransactionResponse response =
                transactionService.executeTransfer(
                        request,
                        RequestContext.from(servletRequest)
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @PathVariable UUID accountId,
            HttpServletRequest servletRequest
    ) {
        List<TransactionResponse> transactions =
                transactionService.getTransactions(
                        accountId,
                        RequestContext.from(servletRequest)
                );

        return ResponseEntity.ok(transactions);
    }
}
