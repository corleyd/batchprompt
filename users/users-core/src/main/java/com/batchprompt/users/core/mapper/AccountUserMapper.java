package com.batchprompt.users.core.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.batchprompt.users.core.model.AccountUser;
import com.batchprompt.users.model.dto.AccountUserDto;

@Component
public class AccountUserMapper {

    public AccountUserDto toDto(AccountUser accountUser) {
        return AccountUserDto.builder()
                .accountUuid(accountUser.getAccountUuid())
                .userId(accountUser.getUserId())
                .owner(accountUser.isOwner())
                .createTimestamp(accountUser.getCreateTimestamp())
                .updateTimestamp(accountUser.getUpdateTimestamp())
                .build();
    }

    public AccountUser toEntity(AccountUserDto accountUserDto) {
        return AccountUser.builder()
                .accountUuid(accountUserDto.getAccountUuid())
                .userId(accountUserDto.getUserId())
                .owner(accountUserDto.isOwner())
                .createTimestamp(accountUserDto.getCreateTimestamp())
                .updateTimestamp(accountUserDto.getUpdateTimestamp())
                .build();
    }

    public List<AccountUserDto> toDtoList(List<AccountUser> accountUsers) {
        return accountUsers.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public Page<AccountUserDto> toDtoPage(Page<AccountUser> accountUsers) {
        return accountUsers.map(this::toDto);
    }
}