package com.batchprompt.files.core.repository;


import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.batchprompt.files.core.model.FileEntity;
import com.batchprompt.files.core.model.FileRecord;

@Repository
public interface FileRecordRepository extends JpaRepository<FileRecord, UUID> {
    List<FileRecord> findByFile(FileEntity file);
    List<FileRecord> findByFileFileUuid(UUID fileUuid);
    void deleteByFileFileUuid(UUID fileUuid);
}