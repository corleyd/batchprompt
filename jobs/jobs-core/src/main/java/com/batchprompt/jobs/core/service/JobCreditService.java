package com.batchprompt.jobs.core.service;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.batchprompt.users.client.AccountClient;
import com.batchprompt.users.model.dto.AccountDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing job credit usage calculations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobCreditService {

    private final AccountClient accountClient;
    
    /**
     * Check if a user has sufficient credits for a task
     * 
     * @param userId The ID of the user to check
     * @return true if the user has sufficient credits, false otherwise
     */
    public boolean checkUserHasSufficientCredits(String userId) {
        if (userId == null) {
            log.error("User ID is null when checking for sufficient credits");
            return false;
        }
        
        try {
            // Get all accounts for the user
            ResponseEntity<List<AccountDto>> accountsResponse = accountClient.getUserAccountsByUserId(userId);
            if (!accountsResponse.getStatusCode().is2xxSuccessful() || accountsResponse.getBody() == null || accountsResponse.getBody().isEmpty()) {
                log.error("No accounts found for user ID: {}", userId);
                return false;
            }
            
            // Check each account for available credits
            boolean hasSufficientCredits = false;
            for (AccountDto account : accountsResponse.getBody()) {
                ResponseEntity<Integer> balanceResponse = accountClient.getAccountBalance(account.getAccountUuid());
                if (balanceResponse.getStatusCode().is2xxSuccessful() && balanceResponse.getBody() != null) {
                    int balance = balanceResponse.getBody();
                    if (balance > 0) {
                        hasSufficientCredits = true;
                        log.debug("Account {} has sufficient credits: {}", account.getAccountUuid(), balance);
                        break;
                    }
                }
            }
            
            if (!hasSufficientCredits) {
                log.warn("User {} has insufficient credits available on all accounts", userId);
            }
            
            return hasSufficientCredits;
        } catch (Exception e) {
            log.error("Error checking available credits for user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }
 
    /**
     * Get user accounts by UUID
     * 
     * @param userId The id of the user
     * @return List of account DTOs if found, empty list otherwise
     */
    public List<AccountDto> getUserAccounts(String userId) {
        if (userId == null) {
            log.error("User ID is null when retrieving user accounts");
            return List.of();
        }
        
        try {
            ResponseEntity<List<AccountDto>> response = accountClient.getUserAccountsByUserId(userId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                log.error("Could not retrieve accounts for user: {}", userId);
                return List.of();
            }
        } catch (Exception e) {
            log.error("Error retrieving accounts for user {}: {}", userId, e.getMessage(), e);
            return List.of();
        }
    }
}