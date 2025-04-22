package com.batchprompt.prompts.dto;

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
public class PromptDto {
    private UUID promptUuid;
    private String userId;
    private String name;
    private String description;
    private String promptText;
    private String outputSchema;
    private LocalDateTime createTimestamp;
    private LocalDateTime updateTimestamp;
}