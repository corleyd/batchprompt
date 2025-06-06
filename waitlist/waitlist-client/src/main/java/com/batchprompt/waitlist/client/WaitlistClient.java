package com.batchprompt.waitlist.client;

import com.batchprompt.waitlist.model.dto.WaitlistEntryDto;
import com.batchprompt.waitlist.model.dto.WaitlistSignupDto;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class WaitlistClient {

    private final RestTemplate restTemplate;

    
    @Value("${services.waitlist.url}")
    private String baseUrl;

    public WaitlistEntryDto joinWaitlist(WaitlistSignupDto signupDto) {
        return restTemplate.postForObject(baseUrl + "/join", signupDto, WaitlistEntryDto.class);
    }

    public Optional<WaitlistEntryDto> getWaitlistStatus(String email) {
        try {
            ResponseEntity<WaitlistEntryDto> response = restTemplate.getForEntity(
                baseUrl + "/status?email=" + email, WaitlistEntryDto.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<Integer> getWaitlistPosition(String email) {
        try {
            ResponseEntity<Integer> response = restTemplate.getForEntity(
                baseUrl + "/position?email=" + email, Integer.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void markAsRegistered(String email) {
        restTemplate.postForObject(baseUrl + "/register?email=" + email, null, Void.class);
    }

    // Admin methods
    public List<WaitlistEntryDto> getAllEntries() {
        ResponseEntity<List<WaitlistEntryDto>> response = restTemplate.exchange(
            baseUrl + "/api/waitlist/admin/entries",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<WaitlistEntryDto>>() {}
        );
        return response.getBody();
    }

    public List<WaitlistEntryDto> getPendingEntries() {
        ResponseEntity<List<WaitlistEntryDto>> response = restTemplate.exchange(
            baseUrl + "/api/waitlist/admin/pending",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<WaitlistEntryDto>>() {}
        );
        return response.getBody();
    }

    public WaitlistEntryDto inviteUser(UUID entryId) {
        return restTemplate.postForObject(
            baseUrl + "/api/waitlist/admin/invite/" + entryId, 
            null, 
            WaitlistEntryDto.class
        );
    }

    public List<WaitlistEntryDto> inviteNextUsers(int count) {
        ResponseEntity<List<WaitlistEntryDto>> response = restTemplate.exchange(
            baseUrl + "/api/waitlist/admin/invite-next?count=" + count,
            HttpMethod.POST,
            null,
            new ParameterizedTypeReference<List<WaitlistEntryDto>>() {}
        );
        return response.getBody();
    }
}