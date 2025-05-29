package com.batchprompt.prompts.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

import com.batchprompt.prompts.model.PromptOutputMethod;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptDto {
    private UUID promptUuid;
    private String userId;
    private String name;
    private String description;
    private String promptText;
    private PromptOutputMethod outputMethod;
    private String responseJsonSchema;
    private String responseTextColumnName;
    private LocalDateTime createTimestamp;
    private LocalDateTime updateTimestamp;
    private Integer jobRunCount;
    private LocalDateTime lastJobRunTimestamp;
    private LocalDateTime deleteTimestamp;    
}