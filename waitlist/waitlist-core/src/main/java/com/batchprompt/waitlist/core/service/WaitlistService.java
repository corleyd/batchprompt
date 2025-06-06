package com.batchprompt.waitlist.core.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batchprompt.waitlist.core.mapper.WaitlistEntryMapper;
import com.batchprompt.waitlist.core.model.WaitlistEntry;
import com.batchprompt.waitlist.core.repository.WaitlistEntryRepository;
import com.batchprompt.waitlist.model.WaitlistStatus;
import com.batchprompt.waitlist.model.dto.WaitlistEntryDto;
import com.batchprompt.waitlist.model.dto.WaitlistSignupDto;

@Service
@Transactional
public class WaitlistService {

    private final WaitlistEntryRepository waitlistEntryRepository;
    private final WaitlistEntryMapper waitlistEntryMapper;
    
    @Autowired(required = false)
    private EmailService emailService;

    public WaitlistService(WaitlistEntryRepository waitlistEntryRepository, 
                          WaitlistEntryMapper waitlistEntryMapper) {
        this.waitlistEntryRepository = waitlistEntryRepository;
        this.waitlistEntryMapper = waitlistEntryMapper;
    }

    public WaitlistEntryDto joinWaitlist(WaitlistSignupDto signupDto) {
        Optional<WaitlistEntry> existingEntry = waitlistEntryRepository.findByEmail(signupDto.getEmail());
        
        if (existingEntry.isPresent()) {
            // Update existing entry if user signs up again
            WaitlistEntry entry = waitlistEntryMapper.updateEntity(existingEntry.get(), signupDto);
            entry = waitlistEntryRepository.save(entry);
            WaitlistEntryDto dto = waitlistEntryMapper.toDto(entry);
            
            // Send signup email for updated entry
            sendSignupEmail(dto);
            
            return dto;
        }

        // Create new entry
        WaitlistEntry entry = waitlistEntryMapper.toEntity(signupDto);
        entry.setPosition(getNextPosition());
        entry = waitlistEntryRepository.save(entry);
        
        WaitlistEntryDto dto = waitlistEntryMapper.toDto(entry);
        
        // Send signup email for new entry
        sendSignupEmail(dto);
        
        return dto;
    }

    @Transactional(readOnly = true)
    public Optional<WaitlistEntryDto> findByEmail(String email) {
        return waitlistEntryRepository.findByEmail(email)
                .map(waitlistEntryMapper::toDto);
    }

    @Transactional(readOnly = true)
    public int getWaitlistPosition(String email) {
        Optional<WaitlistEntry> entry = waitlistEntryRepository.findByEmail(email);
        if (entry.isEmpty() || entry.get().getStatus() != WaitlistStatus.PENDING) {
            return -1;
        }
        
        return waitlistEntryRepository.countPendingEntriesBeforeEmail(WaitlistStatus.PENDING, email) + 1;
    }

    @Transactional(readOnly = true)
    public List<WaitlistEntryDto> getAllEntries() {
        return waitlistEntryRepository.findAll()
                .stream()
                .map(waitlistEntryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WaitlistEntryDto> getPendingEntries() {
        return waitlistEntryRepository.findByStatusOrderByCreatedAtAsc(WaitlistStatus.PENDING)
                .stream()
                .map(waitlistEntryMapper::toDto)
                .collect(Collectors.toList());
    }

    public WaitlistEntryDto inviteUser(UUID entryId) {
        WaitlistEntry entry = waitlistEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Waitlist entry not found"));
        
        entry.setStatus(WaitlistStatus.INVITED);
        entry.setInvitedAt(LocalDateTime.now());
        
        entry = waitlistEntryRepository.save(entry);
        
        WaitlistEntryDto dto = waitlistEntryMapper.toDto(entry);
        
        // Send invitation email
        sendInvitationEmail(dto);
        
        return dto;
    }

    public void markAsRegistered(String email) {
        Optional<WaitlistEntry> entry = waitlistEntryRepository.findByEmail(email);
        if (entry.isPresent() && entry.get().getStatus() == WaitlistStatus.INVITED) {
            WaitlistEntry waitlistEntry = entry.get();
            waitlistEntry.setStatus(WaitlistStatus.REGISTERED);
            waitlistEntry.setRegisteredAt(LocalDateTime.now());
            waitlistEntryRepository.save(waitlistEntry);
        }
    }

    public List<WaitlistEntryDto> inviteNextUsers(int count) {
        List<WaitlistEntry> pendingEntries = waitlistEntryRepository
                .findByStatusOrderByPosition(WaitlistStatus.PENDING);
        
        return pendingEntries.stream()
                .limit(count)
                .map(entry -> {
                    entry.setStatus(WaitlistStatus.INVITED);
                    entry.setInvitedAt(LocalDateTime.now());
                    return waitlistEntryRepository.save(entry);
                })
                .map(waitlistEntryMapper::toDto)
                .map(dto -> {
                    // Send invitation email for each invited user
                    sendInvitationEmail(dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private Integer getNextPosition() {
        Integer maxPosition = waitlistEntryRepository.findMaxPosition();
        return (maxPosition != null) ? maxPosition + 1 : 1;
    }
    
    private void sendSignupEmail(WaitlistEntryDto entry) {
        if (emailService != null) {
            emailService.sendWaitlistSignupEmail(entry.getEmail(), entry.getName());
        }
    }
    
    private void sendInvitationEmail(WaitlistEntryDto entry) {
        if (emailService != null) {
            emailService.sendWaitlistInvitationEmail(entry.getEmail(), entry.getName(), entry.getCompany());
        }
    }
}