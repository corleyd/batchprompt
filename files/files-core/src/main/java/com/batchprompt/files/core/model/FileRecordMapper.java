package com.batchprompt.files.core.model;

import com.batchprompt.files.model.dto.FileRecordDto;

public class FileRecordMapper {
    
    public static FileRecordDto toDto(FileRecord fileRecord) {
        return FileRecordDto.builder()
                .fileRecordUuid(fileRecord.getFileRecordUuid())
                .fileUuid(fileRecord.getFile().getFileUuid())
                .recordNumber(fileRecord.getRecordNumber())
                .record(fileRecord.getRecord())
                .build();
    }

    public static FileRecord toEntity(FileRecordDto dto) {
        return FileRecord.builder()
                .fileRecordUuid(dto.getFileRecordUuid())
                .file(FileEntity.builder().fileUuid(dto.getFileUuid()).build())
                .recordNumber(dto.getRecordNumber())
                .record(dto.getRecord())
                .build();
    }
}
