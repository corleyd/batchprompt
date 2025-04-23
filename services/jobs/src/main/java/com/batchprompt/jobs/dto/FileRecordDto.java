package com.batchprompt.jobs.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileRecordDto {
    private UUID fileRecordUuid;
    private UUID fileUuid;
    private Integer recordNumber;
    private Object record;
}