package com.batchprompt.files.repository;

import com.batchprompt.files.model.File;
import com.batchprompt.files.model.FileRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileRecordRepository extends JpaRepository<FileRecord, UUID> {
    List<FileRecord> findByFile(File file);
    List<FileRecord> findByFileFileUuid(UUID fileUuid);
    void deleteByFileFileUuid(UUID fileUuid);
}