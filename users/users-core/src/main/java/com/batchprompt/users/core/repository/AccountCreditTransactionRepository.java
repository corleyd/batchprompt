package com.batchprompt.users.core.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.batchprompt.users.core.model.AccountCreditTransaction;

@Repository
public interface AccountCreditTransactionRepository extends JpaRepository<AccountCreditTransaction, UUID> {
    List<AccountCreditTransaction> findByAccountUuid(UUID accountUuid);
    
    @Query("SELECT SUM(t.changeAmount) FROM AccountCreditTransaction t WHERE t.accountUuid = :accountUuid")
    Integer getAccountBalance(UUID accountUuid);
}