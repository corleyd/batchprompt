package com.batchprompt.waitlist.api.controller;

import com.batchprompt.waitlist.core.service.WaitlistService;
import com.batchprompt.waitlist.model.dto.WaitlistEntryDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}