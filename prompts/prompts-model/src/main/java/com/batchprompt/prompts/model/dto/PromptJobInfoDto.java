package com.batchprompt.prompts.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptJobInfoDto {
    private Integer jobRunCountIncrement;
    private LocalDateTime lastJobRunTimestamp;    
}
