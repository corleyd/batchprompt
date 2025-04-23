package com.batchprompt.files.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileRecordDto {
    private UUID fileRecordUuid;
    private UUID fileUuid;
    private Integer recordNumber;
    private JsonNode record;
}