package com.batchprompt.files.repository;

import com.batchprompt.files.model.File;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<File, UUID> {
    List<File> findByUserId(String userId);
    List<File> findByUserIdAndFileType(String userId, File.FileType fileType);
    
    // Add methods with pagination support
    Page<File> findByUserId(String userId, Pageable pageable);
    Page<File> findByUserIdAndFileType(String userId, File.FileType fileType, Pageable pageable);
    Page<File> findByUserIdAndStatus(String userId, File.Status status, Pageable pageable);
    Page<File> findByUserIdAndFileTypeAndStatus(String userId, File.FileType fileType, File.Status status, Pageable pageable);
}