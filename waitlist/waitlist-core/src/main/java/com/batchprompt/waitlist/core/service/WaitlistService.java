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
import com.batchprompt.waitlist.core.mapper.WaitlistAutoAcceptanceMapper;
import com.batchprompt.waitlist.core.model.WaitlistEntry;
import com.batchprompt.waitlist.core.model.WaitlistAutoAcceptance;
import com.batchprompt.waitlist.core.repository.WaitlistEntryRepository;
import com.batchprompt.waitlist.core.repository.WaitlistAutoAcceptanceRepository;
import com.batchprompt.waitlist.model.WaitlistStatus;
import com.batchprompt.waitlist.model.dto.WaitlistEntryDto;
import com.batchprompt.waitlist.model.dto.WaitlistSignupDto;
import com.batchprompt.waitlist.model.dto.WaitlistAutoAcceptanceDto;
import com.batchprompt.waitlist.model.dto.SetAutoAcceptanceCountDto;

@Service
@Transactional
public class WaitlistService {

    private final WaitlistEntryRepository waitlistEntryRepository;
    private final WaitlistEntryMapper waitlistEntryMapper;
    private final WaitlistAutoAcceptanceRepository waitlistAutoAcceptanceRepository;
    private final WaitlistAutoAcceptanceMapper waitlistAutoAcceptanceMapper;
    
    @Autowired(required = false)
    private EmailService emailService;

    public WaitlistService(WaitlistEntryRepository waitlistEntryRepository, 
                          WaitlistEntryMapper waitlistEntryMapper,
                          WaitlistAutoAcceptanceRepository waitlistAutoAcceptanceRepository,
                          WaitlistAutoAcceptanceMapper waitlistAutoAcceptanceMapper) {
        this.waitlistEntryRepository = waitlistEntryRepository;
        this.waitlistEntryMapper = waitlistEntryMapper;
        this.waitlistAutoAcceptanceRepository = waitlistAutoAcceptanceRepository;
        this.waitlistAutoAcceptanceMapper = waitlistAutoAcceptanceMapper;
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
        
        // Check if auto-acceptance is enabled and automatically accept if possible
        boolean autoAccepted = tryAutoAcceptance(entry);
        
        entry = waitlistEntryRepository.save(entry);
        
        WaitlistEntryDto dto = waitlistEntryMapper.toDto(entry);
        
        // Send appropriate email based on status
        if (autoAccepted) {
            sendInvitationEmail(dto);
        } else {
            sendSignupEmail(dto);
        }
        
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
    
    /**
     * Try to automatically accept a waitlist entry if auto-acceptance is enabled
     * @param entry The waitlist entry to potentially auto-accept
     * @return true if the entry was auto-accepted, false otherwise
     */
    private boolean tryAutoAcceptance(WaitlistEntry entry) {
        Optional<WaitlistAutoAcceptance> configOpt = waitlistAutoAcceptanceRepository.findCurrentConfiguration();
        
        if (configOpt.isEmpty()) {
            return false;
        }
        
        WaitlistAutoAcceptance config = configOpt.get();
        
        // Try to atomically decrement the count
        int rowsAffected = waitlistAutoAcceptanceRepository.decrementAutoAcceptCount(config.getId());
        
        if (rowsAffected > 0) {
            // Successfully decremented, auto-accept the user
            entry.setStatus(WaitlistStatus.INVITED);
            entry.setInvitedAt(LocalDateTime.now());
            return true;
        }
        
        return false;
    }
    
    /**
     * Get current auto-acceptance configuration
     */
    public WaitlistAutoAcceptanceDto getAutoAcceptanceConfiguration() {
        return waitlistAutoAcceptanceRepository.findCurrentConfiguration()
                .map(waitlistAutoAcceptanceMapper::toDto)
                .orElse(WaitlistAutoAcceptanceDto.builder()
                        .remainingAutoAcceptCount(0)
                        .build());
    }
    
    /**
     * Set the number of users to auto-accept
     */
    public WaitlistAutoAcceptanceDto setAutoAcceptanceCount(SetAutoAcceptanceCountDto request, String adminUserId) {
        WaitlistAutoAcceptance config = waitlistAutoAcceptanceRepository.findCurrentConfiguration()
                .orElse(WaitlistAutoAcceptance.builder()
                        .createdBy(adminUserId)
                        .build());
        
        config.setRemainingAutoAcceptCount(request.getCount());
        config.setNotes(request.getNotes());
        config.setCreatedBy(adminUserId);
        config = waitlistAutoAcceptanceRepository.save(config);
        
        return waitlistAutoAcceptanceMapper.toDto(config);
    }
}