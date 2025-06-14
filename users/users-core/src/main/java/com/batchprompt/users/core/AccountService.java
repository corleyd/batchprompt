package com.batchprompt.users.core;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batchprompt.notifications.client.NotificationSender;
import com.batchprompt.users.core.model.Account;
import com.batchprompt.users.core.model.AccountCreditTransaction;
import com.batchprompt.users.core.model.AccountUser;
import com.batchprompt.users.core.model.User;
import com.batchprompt.users.core.repository.AccountCreditTransactionRepository;
import com.batchprompt.users.core.repository.AccountRepository;
import com.batchprompt.users.core.repository.AccountUserRepository;
import com.batchprompt.users.core.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountUserRepository accountUserRepository;
    private final AccountCreditTransactionRepository accountCreditTransactionRepository;
    private final UserRepository userRepository;
    private final NotificationSender notificationSender;
    
    private static final double DEFAULT_INITIAL_CREDITS = 750.0;
    private static final String DEFAULT_CREDIT_REASON = "Initial account credits";

    /**
     * Get all accounts
     */
    public Page<Account> getAllAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable);
    }

    /**
     * Get an account by UUID
     */
    public Optional<Account> getAccountById(UUID accountUuid) {
        return accountRepository.findById(accountUuid);
    }
    
    /**
     * Get an account by name
     */
    public Optional<Account> getAccountByName(String name) {
        return accountRepository.findByName(name);
    }
    
    /**
     * Get accounts for a user
     */
    public List<Account> getAccountsForUser(String userId) {
        List<AccountUser> accountUsers = accountUserRepository.findByUserId(userId);
        return accountUsers.stream()
                .map(AccountUser::getAccountUuid)
                .map(accountRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private User getUserForAccount(Account account) {
        List<AccountUser> accountUsers = accountUserRepository.findByAccountUuid(account.getAccountUuid());
        if (accountUsers.isEmpty()) {
            return null; // No users associated with this account
        }
        // Return the first user (assuming one owner per account)
        AccountUser accountUser = accountUsers.get(0);
        
        // The User relationship might be null due to timing/lazy loading issues
        // In this case, we should fetch the user by ID directly
        if (accountUser.getUser() != null) {
            return accountUser.getUser();
        } else {
            // Fallback: lookup the user by userId
            return userRepository.findById(accountUser.getUserId()).orElse(null);
        }
    }
    
    /**
     * Create a new account with default credits
     */
    @Transactional
    public Account createAccount(String accountName, User owner) {
        if (accountRepository.findByName(accountName).isPresent()) {
            throw new IllegalArgumentException("Account with this name already exists");
        }
        
        // Create account
        Account account = new Account();
        account.setAccountUuid(UUID.randomUUID());
        account.setName(accountName);
        LocalDateTime now = LocalDateTime.now();
        account.setCreateTimestamp(now);
        account.setUpdateTimestamp(now);
        
        Account savedAccount = accountRepository.save(account);
        
        // Associate owner with account
        createAccountUserAssociation(savedAccount, owner, true);
        
        // Add initial credits to account
        addCreditsToAccount(
            savedAccount.getAccountUuid(), 
            DEFAULT_INITIAL_CREDITS, 
            DEFAULT_CREDIT_REASON, 
            null,
            owner  // Pass the owner directly to avoid lookup issues
        );
        
        return savedAccount;
    }

    /**
     * Create account user association
     */
    @Transactional
    public AccountUser createAccountUserAssociation(Account account, User user, boolean isOwner) {
        AccountUser accountUser = new AccountUser();
        accountUser.setAccountUuid(account.getAccountUuid());
        accountUser.setUserId(user.getUserId());
        accountUser.setOwner(isOwner);
        LocalDateTime now = LocalDateTime.now();
        accountUser.setCreateTimestamp(now);
        accountUser.setUpdateTimestamp(now);
        
        return accountUserRepository.save(accountUser);
    }
    
    /**
     * Add credits to an account
     */
    @Transactional
    public AccountCreditTransaction addCreditsToAccount(UUID accountUuid, Double amount, String reason, String referenceId) {
        if (!accountRepository.existsById(accountUuid)) {
            throw new IllegalArgumentException("Account not found");
        }
        
        Account account = accountRepository.findById(accountUuid)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        AccountCreditTransaction transaction = new AccountCreditTransaction();
        transaction.setTransactionUuid(UUID.randomUUID());
        transaction.setAccountUuid(accountUuid);
        transaction.setChangeAmount(amount);
        transaction.setReason(reason);
        transaction.setReferenceId(referenceId);
        transaction.setCreateTimestamp(LocalDateTime.now());

        transaction = accountCreditTransactionRepository.save(transaction);

        // Send notification if we can find a user for the account
        User user = getUserForAccount(account);
        if (user != null) {
            notificationSender.send("account/balance", 
                new AccountBalanceDto(accountUuid, getAccountBalance(accountUuid)), user.getUserId());
        }
        
        return transaction;
    }
    
    /**
     * Add credits to an account with a specific user for notifications
     */
    @Transactional
    public AccountCreditTransaction addCreditsToAccount(UUID accountUuid, Double amount, String reason, String referenceId, User user) {
        if (!accountRepository.existsById(accountUuid)) {
            throw new IllegalArgumentException("Account not found");
        }

        AccountCreditTransaction transaction = new AccountCreditTransaction();
        transaction.setTransactionUuid(UUID.randomUUID());
        transaction.setAccountUuid(accountUuid);
        transaction.setChangeAmount(amount);
        transaction.setReason(reason);
        transaction.setReferenceId(referenceId);
        transaction.setCreateTimestamp(LocalDateTime.now());

        transaction = accountCreditTransactionRepository.save(transaction);

        // Send notification if user is provided
        if (user != null) {
            notificationSender.send("account/balance", 
                new AccountBalanceDto(accountUuid, getAccountBalance(accountUuid)), user.getUserId());
        }
        
        return transaction;
    }
    
    /**
     * Get account balance
     */
    public Double getAccountBalance(UUID accountUuid) {
        Double balance = accountCreditTransactionRepository.getAccountBalance(accountUuid);
        return balance != null ? balance : 0;
    }
    
    /**
     * Get credit transactions for an account
     */
    public List<AccountCreditTransaction> getAccountTransactions(UUID accountUuid) {
        return accountCreditTransactionRepository.findByAccountUuid(accountUuid);
    }
    
    /**
     * Update account details
     */
    @Transactional
    public Optional<Account> updateAccount(UUID accountUuid, String name) {
        return accountRepository.findById(accountUuid)
                .map(account -> {
                    // Check if name is already taken by another account
                    if (name != null && !name.equals(account.getName())) {
                        accountRepository.findByName(name)
                            .ifPresent(existingAccount -> {
                                if (!existingAccount.getAccountUuid().equals(accountUuid)) {
                                    throw new IllegalArgumentException("Account name already in use");
                                }
                            });
                        account.setName(name);
                    }
                    
                    account.setUpdateTimestamp(LocalDateTime.now());
                    return accountRepository.save(account);
                });
    }

    public record AccountBalanceDto(UUID accountUuid, Double balance) {}
}