package com.batchprompt.jobs.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDto {
    private UUID fileUuid;
    private String fileType;
    private String userId;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String status;
    private UUID resultFileUuid;
    private Object validationErrors;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}