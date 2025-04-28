package com.batchprompt.files.core.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.batchprompt.files.core.model.FileEntity;
import com.batchprompt.files.model.FileStatus;
import com.batchprompt.files.model.FileType;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, UUID> {
    List<FileEntity> findByUserId(String userId);
    List<FileEntity> findByUserIdAndFileType(String userId, FileType fileType);
    
    // Add methods with pagination support
    Page<FileEntity> findByUserId(String userId, Pageable pageable);
    Page<FileEntity> findByUserIdAndFileType(String userId, FileType fileType, Pageable pageable);
    Page<FileEntity> findByUserIdAndStatus(String userId, FileStatus status, Pageable pageable);
    Page<FileEntity> findByUserIdAndFileTypeAndStatus(String userId, FileType fileType, FileStatus status, Pageable pageable);
}