package com.batchprompt.jobs.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelCreditUsageRequestDto {
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer thinkingTokens;
    private LocalDateTime timestamp;
}
