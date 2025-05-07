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
public class AccountDto {
    private UUID accountUuid;
    private String name;
    private LocalDateTime createTimestamp;
    private LocalDateTime updateTimestamp;
}