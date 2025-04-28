package com.batchprompt.files.model.dto;

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
    private FileDto file;
    private String fieldName;
    private String fieldType;
    private String description;
    private int fieldOrder;
}
