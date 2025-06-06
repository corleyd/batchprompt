package com.batchprompt.waitlist.core.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.batchprompt.waitlist.core.model.WaitlistEntry;
import com.batchprompt.waitlist.model.WaitlistStatus;
import com.batchprompt.waitlist.model.dto.WaitlistEntryDto;
import com.batchprompt.waitlist.model.dto.WaitlistSignupDto;

@Component
public class WaitlistEntryMapper {

    public WaitlistEntry toEntity(WaitlistSignupDto signupDto) {
        return WaitlistEntry.builder()
                .email(signupDto.getEmail())
                .name(signupDto.getName())
                .company(signupDto.getCompany())
                .useCase(signupDto.getUseCase())
                .status(WaitlistStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public WaitlistEntryDto toDto(WaitlistEntry entity) {
        return new WaitlistEntryDto(
                entity.getId(),
                entity.getEmail(),
                entity.getName(),
                entity.getCompany(),
                entity.getUseCase(),
                entity.getPosition(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getInvitedAt(),
                entity.getRegisteredAt()
        );
    }

    public WaitlistEntry updateEntity(WaitlistEntry entity, WaitlistSignupDto signupDto) {
        entity.setName(signupDto.getName());
        entity.setCompany(signupDto.getCompany());
        entity.setUseCase(signupDto.getUseCase());
        return entity;
    }
}