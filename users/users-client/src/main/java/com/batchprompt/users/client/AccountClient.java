package com.batchprompt.users.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.batchprompt.users.client.config.FeignClientConfig;
import com.batchprompt.users.model.dto.AccountCreditTransactionDto;
import com.batchprompt.users.model.dto.AccountDto;

@FeignClient(
    name = "users-account-api", 
    url = "${users-account-api.url}", 
    path = "/api/accounts",
    configuration = FeignClientConfig.class
)
public interface AccountClient {

    @GetMapping("/user")
    ResponseEntity<List<AccountDto>> getAccountsForCurrentUser();
    
    @GetMapping("/{accountUuid}")
    ResponseEntity<AccountDto> getAccountById(@PathVariable UUID accountUuid);
    
    @GetMapping("/{accountUuid}/balance")
    ResponseEntity<Integer> getAccountBalance(@PathVariable UUID accountUuid);
    
    @GetMapping("/{accountUuid}/transactions")
    ResponseEntity<List<AccountCreditTransactionDto>> getAccountTransactions(@PathVariable UUID accountUuid);
    
    @PostMapping
    ResponseEntity<AccountDto> createAccount(@RequestBody AccountDto accountDto);
    
    @PutMapping("/{accountUuid}")
    ResponseEntity<AccountDto> updateAccount(@PathVariable UUID accountUuid, @RequestBody AccountDto accountDto);
    
    @PostMapping("/{accountUuid}/credits")
    ResponseEntity<AccountCreditTransactionDto> addCredits(
            @PathVariable UUID accountUuid,
            @RequestBody AccountCreditTransactionDto transactionDto);
}