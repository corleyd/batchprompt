package com.batchprompt.files.mapper;

import com.batchprompt.files.dto.FileDto;
import com.batchprompt.files.dto.FileRecordDto;
import com.batchprompt.files.model.File;
import com.batchprompt.files.model.FileRecord;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FileMapper {

    public FileDto toDto(File file) {
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

    public FileRecordDto toDto(FileRecord fileRecord) {
        return FileRecordDto.builder()
                .fileRecordUuid(fileRecord.getFileRecordUuid())
                .fileUuid(fileRecord.getFile().getFileUuid())
                .recordNumber(fileRecord.getRecordNumber())
                .record(fileRecord.getRecord())
                .build();
    }

    public List<FileDto> toDtoList(List<File> files) {
        return files.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<FileRecordDto> toRecordDtoList(List<FileRecord> records) {
        return records.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}