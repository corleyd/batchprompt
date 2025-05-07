package com.batchprompt.users.core.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.batchprompt.users.core.model.AccountUser;
import com.batchprompt.users.core.model.AccountUserPK;

@Repository
public interface AccountUserRepository extends JpaRepository<AccountUser, AccountUserPK> {
    List<AccountUser> findByUserUuid(UUID userUuid);
    List<AccountUser> findByAccountUuid(UUID accountUuid);
}