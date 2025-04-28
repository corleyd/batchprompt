package com.batchprompt.files.core.service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batchprompt.files.core.model.FileEntity;
import com.batchprompt.files.core.model.FileFieldEntity;
import com.batchprompt.files.core.model.FileFieldMapper;
import com.batchprompt.files.core.repository.FileFieldRepository;
import com.batchprompt.files.core.repository.FileRepository;
import com.batchprompt.files.model.dto.FileFieldDto;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileFieldService {
    
    private final FileFieldRepository fileFieldRepository;
    private final FileRepository fileRepository;
    
    /**
     * Get all fields for a specific file in order
     * 
     * @param fileUuid the UUID of the file
     * @return list of file fields in order
     */
    public List<FileFieldDto> getFileFields(UUID fileUuid) {
        return fileFieldRepository.findByFileFileUuidOrderByFieldOrder(fileUuid).stream()
                .map(FileFieldMapper::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Add a single field to a file
     * 
     * @param fileUuid the UUID of the file
     * @param fieldName the name of the field
     * @param fieldType the type of the field
     * @param description optional description of the field
     * @return the created FileFieldEntity
     */
    @Transactional
    public FileFieldDto addFieldToFile(UUID fileUuid, String fieldName, String fieldType, String description) {
        FileEntity file = fileRepository.findById(fileUuid)
                .orElseThrow(() -> new EntityNotFoundException("File not found with UUID: " + fileUuid));
        
        // Get current highest order number for this file, or -1 if no fields exist
        Integer maxOrder = fileFieldRepository.findByFileFileUuidOrderByFieldOrder(fileUuid).stream()
                .map(FileFieldEntity::getFieldOrder)
                .max(Integer::compareTo)
                .orElse(-1);
        
        // New field gets the next order number
        FileFieldEntity newField = FileFieldEntity.builder()
                .file(file)
                .fieldName(fieldName)
                .fieldType(fieldType)
                .fieldOrder(maxOrder + 1)
                .description(description)
                .build();
        
        fileFieldRepository.save(newField);
        return FileFieldMapper.toDto(newField);
    }
   
    /**
     * Add multiple fields to a file at once
     * 
     * @param fileUuid the UUID of the file
     * @param fields list of field information (name, type, description)
     * @return list of created fields
     */
    @Transactional
    public List<FileFieldDto> addFieldsToFile(UUID fileUuid, List<FileFieldDto> fields) {
        FileEntity file = fileRepository.findById(fileUuid)
                .orElseThrow(() -> new EntityNotFoundException("File not found with UUID: " + fileUuid));
        
        List<FileFieldEntity> existingFields = fileFieldRepository.findByFileFileUuidOrderByFieldOrder(fileUuid);

        AtomicInteger startOrder = new AtomicInteger(existingFields.isEmpty() ? 0 : 
                existingFields.stream()
                        .mapToInt(FileFieldEntity::getFieldOrder)
                        .max()
                        .orElse(-1) + 1);
        
        List<FileFieldEntity> newFields = fields.stream()
                .map(dto -> {
                        var entity = FileFieldMapper.toEntity(dto);
                        entity.setFile(file);
                        entity.setFieldOrder(startOrder.getAndIncrement());
                        return entity;
                })
                .collect(Collectors.toList());

        return fileFieldRepository.saveAll(newFields).stream()
                .map(field -> FileFieldMapper.toDto(field))
                .collect(Collectors.toList());
    }
    
    /**
     * Remove a field from a file
     * 
     * @param fieldUuid the UUID of the field to remove
     */
    @Transactional
    public void removeField(UUID fieldUuid) {
        FileFieldEntity field = fileFieldRepository.findById(fieldUuid)
                .orElseThrow(() -> new EntityNotFoundException("Field not found with UUID: " + fieldUuid));
        
        UUID fileUuid = field.getFile().getFileUuid();
        fileFieldRepository.delete(field);
        
        // Reorder remaining fields to ensure continuous ordering
        List<FileFieldEntity> remainingFields = fileFieldRepository.findByFileFileUuidOrderByFieldOrder(fileUuid);
        for (int i = 0; i < remainingFields.size(); i++) {
            FileFieldEntity remainingField = remainingFields.get(i);
            remainingField.setFieldOrder(i);
            fileFieldRepository.save(remainingField);
        }
    }
}    
