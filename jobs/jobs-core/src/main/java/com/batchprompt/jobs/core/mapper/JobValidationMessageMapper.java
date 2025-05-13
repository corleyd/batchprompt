package com.batchprompt.jobs.core.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.batchprompt.jobs.core.model.JobValidationResultMessage;
import com.batchprompt.jobs.model.dto.JobValidationResultMessageDto;

/**
 * Mapper for converting between JobValidationMessageEntity and JobValidationMessageDto
 */
@Component
public class JobValidationMessageMapper {

    /**
     * Convert a JobValidationMessageEntity to a JobValidationMessageDto
     *
     * @param entity the entity to convert
     * @return the DTO representation
     */
    public JobValidationResultMessageDto toDto(JobValidationResultMessage entity) {
        if (entity == null) {
            return null;
        }
        
        return JobValidationResultMessageDto.builder()
                .jobValidationMessageUuid(entity.getJobValidationMessageUuid())
                .jobUuid(entity.getJobUuid())
                .jobTaskUuid(entity.getJobTaskUuid())
                .recordNumber(entity.getRecordNumber())
                .fieldName(entity.getFieldName())
                .message(entity.getMessage())
                .build();
    }
    
    /**
     * Convert a JobValidationMessageDto to a JobValidationMessageEntity
     *
     * @param dto the DTO to convert
     * @return the entity representation
     */
    public JobValidationResultMessage toEntity(JobValidationResultMessageDto dto) {
        if (dto == null) {
            return null;
        }
        
        return JobValidationResultMessage.builder()
                .jobValidationMessageUuid(dto.getJobValidationMessageUuid())
                .jobUuid(dto.getJobUuid())
                .jobTaskUuid(dto.getJobTaskUuid())
                .recordNumber(dto.getRecordNumber())
                .fieldName(dto.getFieldName())
                .message(dto.getMessage())
                .build();
    }
    
    /**
     * Convert a list of JobValidationMessageEntity objects to a list of JobValidationMessageDto objects
     *
     * @param entities the list of entities to convert
     * @return the list of DTO representations
     */
    public List<JobValidationResultMessageDto> toDtoList(List<JobValidationResultMessage> entities) {
        if (entities == null) {
            return null;
        }
        
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Convert a list of JobValidationMessageDto objects to a list of JobValidationMessageEntity objects
     *
     * @param dtos the list of DTOs to convert
     * @return the list of entity representations
     */
    public List<JobValidationResultMessage> toEntityList(List<JobValidationResultMessageDto> dtos) {
        if (dtos == null) {
            return null;
        }
        
        return dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}
