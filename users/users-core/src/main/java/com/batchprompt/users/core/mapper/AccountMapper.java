package com.batchprompt.users.core.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.batchprompt.users.core.model.Account;
import com.batchprompt.users.model.dto.AccountDto;

@Component
public class AccountMapper {

    public AccountDto toDto(Account account) {
        return AccountDto.builder()
                .accountUuid(account.getAccountUuid())
                .name(account.getName())
                .createTimestamp(account.getCreateTimestamp())
                .updateTimestamp(account.getUpdateTimestamp())
                .build();
    }

    public Account toEntity(AccountDto accountDto) {
        return Account.builder()
                .accountUuid(accountDto.getAccountUuid())
                .name(accountDto.getName())
                .createTimestamp(accountDto.getCreateTimestamp())
                .updateTimestamp(accountDto.getUpdateTimestamp())
                .build();
    }

    public List<AccountDto> toDtoList(List<Account> accounts) {
        return accounts.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public Page<AccountDto> toDtoPage(Page<Account> accounts) {
        return accounts.map(this::toDto);
    }
}