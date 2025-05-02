package com.batchprompt.files.core.model;

import com.batchprompt.files.model.dto.FileFieldDto;

public class FileFieldMapper {
    
    public static FileFieldDto toDto(FileFieldEntity entity) {
        return FileFieldDto.builder()
                .file(FileMapper.toDto(entity.getFile()))
                .fieldUuid(entity.getFieldUuid())
                .fieldName(entity.getFieldName())
                .fieldType(entity.getFieldType())
                .description(entity.getDescription())
                .fieldOrder(entity.getFieldOrder())
                .build();
    }

    public static FileFieldEntity toEntity(FileFieldDto dto) {
        return FileFieldEntity.builder()
                .fieldUuid(dto.getFieldUuid())
                .fieldName(dto.getFieldName())
                .fieldType(dto.getFieldType())
                .description(dto.getDescription())
                .fieldOrder(dto.getFieldOrder())
                .build();
    }
}
