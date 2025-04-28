package com.batchprompt.files.core.model;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.batchprompt.files.model.dto.FileDto;
import com.batchprompt.files.model.dto.FileRecordDto;

@Component
public class FileMapper {

    public static FileDto toDto(FileEntity file) {
        return FileDto.builder()
                .fileUuid(file.getFileUuid())
                .fileType(file.getFileType())
                .userId(file.getUserId())
                .fileName(file.getFileName())
                .contentType(file.getContentType())
                .fileSize(file.getFileSize())
                .status(file.getStatus())
                .validationErrors(file.getValidationErrors())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }

    public static FileRecordDto toDto(FileRecord fileRecord) {
        return FileRecordDto.builder()
                .fileRecordUuid(fileRecord.getFileRecordUuid())
                .fileUuid(fileRecord.getFile().getFileUuid())
                .recordNumber(fileRecord.getRecordNumber())
                .record(fileRecord.getRecord())
                .build();
    }

    public List<FileDto> toDtoList(List<FileEntity> files) {
        return files.stream()
                .map(FileMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<FileRecordDto> toRecordDtoList(List<FileRecord> records) {
        return records.stream()
                .map(FileRecordMapper::toDto)
                .collect(Collectors.toList());
    }
}