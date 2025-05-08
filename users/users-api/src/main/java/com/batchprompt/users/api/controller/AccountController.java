package com.batchprompt.users.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.batchprompt.users.core.AccountService;
import com.batchprompt.users.core.UserService;
import com.batchprompt.users.core.mapper.AccountCreditTransactionMapper;
import com.batchprompt.users.core.mapper.AccountMapper;
import com.batchprompt.users.core.model.Account;
import com.batchprompt.users.core.model.AccountCreditTransaction;
import com.batchprompt.users.model.dto.AccountCreditTransactionDto;
import com.batchprompt.users.model.dto.AccountDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;
    private final UserService userService;
    private final AccountMapper accountMapper;
    private final AccountCreditTransactionMapper transactionMapper;

    @GetMapping
    public ResponseEntity<Page<AccountDto>> getAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Account> accounts = accountService.getAllAccounts(pageable);
        
        return ResponseEntity.ok(accountMapper.toDtoPage(accounts));
    }

    @GetMapping("/{accountUuid}")
    public ResponseEntity<AccountDto> getAccountById(@PathVariable UUID accountUuid) {
        return accountService.getAccountById(accountUuid)
                .map(accountMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/user/{userUuid}")
    public ResponseEntity<List<AccountDto>> getAccountsForUser(@PathVariable UUID userUuid) {
        return ResponseEntity.ok(accountMapper.toDtoList(accountService.getAccountsForUser(userUuid)));
    }
    
    @GetMapping("/{accountUuid}/balance")
    public ResponseEntity<Integer> getAccountBalance(@PathVariable UUID accountUuid) {
        return accountService.getAccountById(accountUuid)
                .map(account -> ResponseEntity.ok(accountService.getAccountBalance(account.getAccountUuid())))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{accountUuid}/transactions")
    public ResponseEntity<List<AccountCreditTransactionDto>> getAccountTransactions(@PathVariable UUID accountUuid) {
        return accountService.getAccountById(accountUuid)
                .map(account -> {
                    List<AccountCreditTransaction> transactions = 
                            accountService.getAccountTransactions(account.getAccountUuid());
                    return ResponseEntity.ok(transactionMapper.toDtoList(transactions));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<AccountDto> createAccount(
            @RequestBody AccountDto accountDto,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId = jwt.getSubject();
        
        return userService.getUserByUserId(userId)
                .map(user -> {
                    try {
                        Account account = accountService.createAccount(accountDto.getName(), user);
                        return ResponseEntity.status(HttpStatus.CREATED)
                                .body(accountMapper.toDto(account));
                    } catch (IllegalArgumentException e) {
                        log.warn("Failed to create account: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.CONFLICT).<AccountDto>build();
                    }
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).<AccountDto>build());
    }
    
    @PutMapping("/{accountUuid}")
    public ResponseEntity<AccountDto> updateAccount(
            @PathVariable UUID accountUuid,
            @RequestBody AccountDto accountDto,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId = jwt.getSubject();
        
        return userService.getUserByUserId(userId)
                .map(user -> {
                    try {
                        return accountService.updateAccount(accountUuid, accountDto.getName())
                                .map(accountMapper::toDto)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().<AccountDto>build());
                    } catch (IllegalArgumentException e) {
                        log.warn("Failed to update account: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.CONFLICT).<AccountDto>build();
                    }
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).<AccountDto>build());
    }
    
    @PostMapping("/{accountUuid}/credits")
    public ResponseEntity<AccountCreditTransactionDto> addCredits(
            @PathVariable UUID accountUuid,
            @RequestBody AccountCreditTransactionDto transactionDto) {
        
        try {
            // Validate the account exists
            if (!accountService.getAccountById(accountUuid).isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            // Add credits to the account
            AccountCreditTransaction transaction = accountService.addCreditsToAccount(
                    accountUuid,
                    transactionDto.getChangeAmount(),
                    transactionDto.getReason(),
                    transactionDto.getReferenceId()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(transactionMapper.toDto(transaction));
        } catch (IllegalArgumentException e) {
            log.warn("Failed to add credits: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}