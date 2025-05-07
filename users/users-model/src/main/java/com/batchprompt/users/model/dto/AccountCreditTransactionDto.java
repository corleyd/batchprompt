package com.batchprompt.users.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreditTransactionDto {
    private UUID transactionUuid;
    private UUID accountUuid;
    private Double changeAmount;
    private String reason;
    private String referenceId;
    private LocalDateTime createTimestamp;
}