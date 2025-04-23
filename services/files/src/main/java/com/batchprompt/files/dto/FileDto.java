package com.batchprompt.files.dto;

import com.batchprompt.files.model.File;
import com.fasterxml.jackson.databind.JsonNode;
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
public class FileDto {
    private UUID fileUuid;
    private File.FileType fileType;
    private String userId;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private File.Status status;
    private JsonNode validationErrors;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}