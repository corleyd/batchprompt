package com.batchprompt.waitlist.api.controller;

import com.batchprompt.waitlist.core.service.WaitlistService;
import com.batchprompt.waitlist.model.dto.WaitlistEntryDto;
import com.batchprompt.waitlist.model.dto.WaitlistAutoAcceptanceDto;
import com.batchprompt.waitlist.model.dto.SetAutoAcceptanceCountDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/waitlist/admin")
@CrossOrigin(origins = "*")
public class WaitlistAdminController {

    private final WaitlistService waitlistService;

    public WaitlistAdminController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @GetMapping("/entries")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<WaitlistEntryDto>> getAllEntries() {
        List<WaitlistEntryDto> entries = waitlistService.getAllEntries();
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<WaitlistEntryDto>> getPendingEntries() {
        List<WaitlistEntryDto> entries = waitlistService.getPendingEntries();
        return ResponseEntity.ok(entries);
    }

    @PostMapping("/invite/{entryId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<WaitlistEntryDto> inviteUser(@PathVariable UUID entryId) {
        WaitlistEntryDto entry = waitlistService.inviteUser(entryId);
        return ResponseEntity.ok(entry);
    }

    @PostMapping("/invite-next")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<WaitlistEntryDto>> inviteNextUsers(@RequestParam(defaultValue = "10") int count) {
        List<WaitlistEntryDto> entries = waitlistService.inviteNextUsers(count);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/auto-acceptance")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<WaitlistAutoAcceptanceDto> getAutoAcceptanceConfiguration() {
        WaitlistAutoAcceptanceDto config = waitlistService.getAutoAcceptanceConfiguration();
        return ResponseEntity.ok(config);
    }

    @PostMapping("/auto-acceptance")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<WaitlistAutoAcceptanceDto> setAutoAcceptanceCount(
            @Valid @RequestBody SetAutoAcceptanceCountDto request,
            Authentication authentication) {
        String adminUserId = authentication.getName();
        WaitlistAutoAcceptanceDto config = waitlistService.setAutoAcceptanceCount(request, adminUserId);
        return ResponseEntity.ok(config);
    }
}