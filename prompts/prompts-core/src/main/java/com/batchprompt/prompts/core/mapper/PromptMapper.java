package com.batchprompt.prompts.core.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.batchprompt.prompts.core.model.Prompt;
import com.batchprompt.prompts.model.PromptOutputMethod;
import com.batchprompt.prompts.model.dto.PromptDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PromptMapper {
    private final ObjectMapper objectMapper;

    public PromptDto toDto(Prompt prompt) {
        String responseJsonSchema = prompt.getOutputMethod() != PromptOutputMethod.TEXT  ? prompt.getResponseJsonSchema().toString() : null;

        return PromptDto.builder()
                .promptUuid(prompt.getPromptUuid())
                .userId(prompt.getUserId())
                .name(prompt.getName())
                .description(prompt.getDescription())
                .promptText(prompt.getPromptText())
                .outputMethod(prompt.getOutputMethod())
                .responseJsonSchema(responseJsonSchema)
                .createTimestamp(prompt.getCreateTimestamp())
                .updateTimestamp(prompt.getUpdateTimestamp())
                .jobRunCount(prompt.getJobRunCount())
                .lastJobRunTimestamp(prompt.getLastJobRunTimestamp())
                .deleteTimestamp(prompt.getDeleteTimestamp())                
                .build();
    }

    public Prompt toEntity(PromptDto promptDto) {
        JsonNode responseJsonSchema = null;
        if (promptDto.getResponseJsonSchema() != null) {
            try {
                responseJsonSchema = objectMapper.readTree(promptDto.getResponseJsonSchema());
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse output schema", e);
            }
        }
        return Prompt.builder()
                .promptUuid(promptDto.getPromptUuid())
                .userId(promptDto.getUserId())
                .name(promptDto.getName())
                .description(promptDto.getDescription())
                .promptText(promptDto.getPromptText())
                .outputMethod(promptDto.getOutputMethod())
                .responseJsonSchema(responseJsonSchema)
                .createTimestamp(promptDto.getCreateTimestamp())
                .updateTimestamp(promptDto.getUpdateTimestamp())
                .jobRunCount(promptDto.getJobRunCount())
                .lastJobRunTimestamp(promptDto.getLastJobRunTimestamp())
                .deleteTimestamp(promptDto.getDeleteTimestamp())                
                .build();
    }

    public List<PromptDto> toDtoList(List<Prompt> prompts) {
        return prompts.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}