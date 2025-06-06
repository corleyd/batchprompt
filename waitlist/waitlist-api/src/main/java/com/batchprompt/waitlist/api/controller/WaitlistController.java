package com.batchprompt.waitlist.api.controller;

import com.batchprompt.waitlist.core.service.WaitlistService;
import com.batchprompt.waitlist.model.dto.WaitlistEntryDto;
import com.batchprompt.waitlist.model.dto.WaitlistSignupDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/waitlist/public")
public class WaitlistController {

    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @PostMapping("/join")
    public ResponseEntity<WaitlistEntryDto> joinWaitlist(@Valid @RequestBody WaitlistSignupDto signupDto) {
        WaitlistEntryDto entry = waitlistService.joinWaitlist(signupDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }

    @GetMapping("/status")
    public ResponseEntity<WaitlistEntryDto> getWaitlistStatus(@RequestParam String email) {
        Optional<WaitlistEntryDto> entry = waitlistService.findByEmail(email);
        return entry.map(e -> ResponseEntity.ok(e))
                   .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/position")
    public ResponseEntity<Integer> getWaitlistPosition(@RequestParam String email) {
        int position = waitlistService.getWaitlistPosition(email);
        if (position == -1) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(position);
    }

    @PostMapping("/register")
    public ResponseEntity<Void> markAsRegistered(@RequestParam String email) {
        waitlistService.markAsRegistered(email);
        return ResponseEntity.ok().build();
    }
}