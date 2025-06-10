package com.batchprompt.waitlist.core.mapper;

import com.batchprompt.waitlist.core.model.WaitlistAutoAcceptance;
import com.batchprompt.waitlist.model.dto.WaitlistAutoAcceptanceDto;
import org.springframework.stereotype.Component;

@Component
public class WaitlistAutoAcceptanceMapper {
    
    public WaitlistAutoAcceptanceDto toDto(WaitlistAutoAcceptance entity) {
        if (entity == null) {
            return null;
        }
        
        return WaitlistAutoAcceptanceDto.builder()
                .id(entity.getId())
                .remainingAutoAcceptCount(entity.getRemainingAutoAcceptCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .notes(entity.getNotes())
                .build();
    }
}
