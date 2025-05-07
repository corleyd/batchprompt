package com.batchprompt.users.core.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.batchprompt.users.core.model.AccountCreditTransaction;
import com.batchprompt.users.model.dto.AccountCreditTransactionDto;

@Component
public class AccountCreditTransactionMapper {

    public AccountCreditTransactionDto toDto(AccountCreditTransaction transaction) {
        return AccountCreditTransactionDto.builder()
                .transactionUuid(transaction.getTransactionUuid())
                .accountUuid(transaction.getAccountUuid())
                .changeAmount(transaction.getChangeAmount())
                .reason(transaction.getReason())
                .referenceId(transaction.getReferenceId())
                .createTimestamp(transaction.getCreateTimestamp())
                .build();
    }

    public AccountCreditTransaction toEntity(AccountCreditTransactionDto transactionDto) {
        return AccountCreditTransaction.builder()
                .transactionUuid(transactionDto.getTransactionUuid())
                .accountUuid(transactionDto.getAccountUuid())
                .changeAmount(transactionDto.getChangeAmount())
                .reason(transactionDto.getReason())
                .referenceId(transactionDto.getReferenceId())
                .createTimestamp(transactionDto.getCreateTimestamp())
                .build();
    }

    public List<AccountCreditTransactionDto> toDtoList(List<AccountCreditTransaction> transactions) {
        return transactions.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public Page<AccountCreditTransactionDto> toDtoPage(Page<AccountCreditTransaction> transactions) {
        return transactions.map(this::toDto);
    }
}