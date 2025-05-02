package com.batchprompt.files.model.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileFieldDto {
/**
 * DTO class for field information
 */
    private UUID fieldUuid;
    private FileDto file;
    private String fieldName;
    private String fieldType;
    private String description;
    private int fieldOrder;
}
