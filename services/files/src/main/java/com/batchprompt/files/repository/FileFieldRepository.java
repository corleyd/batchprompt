package com.batchprompt.files.repository;

import com.batchprompt.files.model.FileField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileFieldRepository extends JpaRepository<FileField, UUID> {
    
    /**
     * Find all fields for a specific file, ordered by field_order
     * 
     * @param fileUuid the UUID of the file
     * @return list of fields in order
     */
    List<FileField> findByFileFileUuidOrderByFieldOrder(UUID fileUuid);
    
    /**
     * Delete all fields associated with a file
     * 
     * @param fileUuid the UUID of the file
     */
    void deleteByFileFileUuid(UUID fileUuid);
    
    /**
     * Check if a file has any fields
     * 
     * @param fileUuid the UUID of the file
     * @return true if fields exist, false otherwise
     */
    boolean existsByFileFileUuid(UUID fileUuid);
}