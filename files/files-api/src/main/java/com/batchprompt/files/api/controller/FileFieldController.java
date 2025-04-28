package com.batchprompt.files.api.controller;


import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.batchprompt.files.core.service.FileFieldService;
import com.batchprompt.files.model.dto.FileFieldDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileFieldController {
    
    private final FileFieldService fileFieldService;
    
    /**
     * Get all fields for a file in order
     * 
     * @param fileUuid the file UUID
     * @return list of fields in order
     */
    @GetMapping("/{fileUuid}/fields")
    public ResponseEntity<List<FileFieldDto>> getFileFields(@PathVariable UUID fileUuid) {
        return ResponseEntity.ok(fileFieldService.getFileFields(fileUuid));
    }
    
    /**
     * Add a field to a file
     * 
     * @param fileUuid the file UUID
     * @param dto the field information
     * @return created field
     */
    @PostMapping("/{fileUuid}/fields")
    public ResponseEntity<FileFieldDto> addField(@PathVariable UUID fileUuid, @RequestBody FileFieldDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fileFieldService.addFieldToFile(fileUuid, dto.getFieldName(), dto.getFieldType(), dto.getDescription()));
    }
    
    /**
     * Add multiple fields to a file at once
     * 
     * @param fileUuid the file UUID
     * @param fields list of field information
     * @return list of created fields
     */
    @PostMapping("/{fileUuid}/fields/batch")
    public ResponseEntity<List<FileFieldDto>> addFields(@PathVariable UUID fileUuid, @RequestBody List<FileFieldDto> fields) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fileFieldService.addFieldsToFile(fileUuid, fields));
    }
   
    /**
     * Delete a field
     * 
     * @param fileUuid the file UUID
     * @param fieldUuid the field UUID
     * @return empty response
     */
    @DeleteMapping("/{fileUuid}/fields/{fieldUuid}")
    public ResponseEntity<Void> removeField(@PathVariable UUID fileUuid, @PathVariable UUID fieldUuid) {
        fileFieldService.removeField(fieldUuid);
        return ResponseEntity.noContent().build();
    }
}