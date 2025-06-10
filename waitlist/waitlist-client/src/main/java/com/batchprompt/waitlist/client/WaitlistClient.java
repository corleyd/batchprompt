package com.batchprompt.waitlist.client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import com.batchprompt.waitlist.model.dto.SetAutoAcceptanceCountDto;
import com.batchprompt.waitlist.model.dto.WaitlistAutoAcceptanceDto;
import com.batchprompt.waitlist.model.dto.WaitlistEntryDto;
import com.batchprompt.waitlist.model.dto.WaitlistSignupDto;

@Component
public class WaitlistClient {

    private final RestTemplate restTemplate;

    private final DefaultUriBuilderFactory factory;

    public WaitlistClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        factory = new DefaultUriBuilderFactory();
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);
        restTemplate.setUriTemplateHandler(factory);
    }
    
    @Value("${services.waitlist.url}")
    private String baseUrl;

    public WaitlistEntryDto joinWaitlist(WaitlistSignupDto signupDto) {
        return restTemplate.postForObject(baseUrl + "/api/waitlist/public/join", signupDto, WaitlistEntryDto.class);
    }

    public Optional<WaitlistEntryDto> getWaitlistStatus(String email) {
        try {
            ResponseEntity<WaitlistEntryDto> response = restTemplate.getForEntity(
                addEmailParameter(baseUrl + "/api/waitlist/public/status", email),
                WaitlistEntryDto.class, 
                email);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<Integer> getWaitlistPosition(String email) {
        try {
            ResponseEntity<Integer> response = restTemplate.getForEntity(
                addEmailParameter(baseUrl + "/api/waitlist/public/position", email), Integer.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void markAsRegistered(String email) {
        restTemplate.postForObject(addEmailParameter(baseUrl + "/api/waitlist/public/register", email), null, Void.class);
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

    public WaitlistAutoAcceptanceDto getAutoAcceptanceConfiguration() {
        return restTemplate.getForObject(
            baseUrl + "/api/waitlist/admin/auto-acceptance", 
            WaitlistAutoAcceptanceDto.class
        );
    }

    public WaitlistAutoAcceptanceDto setAutoAcceptanceCount(SetAutoAcceptanceCountDto request) {
        return restTemplate.postForObject(
            baseUrl + "/api/waitlist/admin/auto-acceptance", 
            request, 
            WaitlistAutoAcceptanceDto.class
        );
    }

    /** 
     * Helper method to add email parameter to URL. This is needed because Spring 
     * the + is being handled incorrectly. See https://stackoverflow.com/questions/50432395/whats-the-proper-way-to-escape-url-variables-with-springs-resttemplate-when-ca
     * 
     * @param url The base URL to which the email parameter will be added
     * @param email The email address to add as a parameter
     * @return The URL with the email parameter added
     * 
     */

    private String addEmailParameter(String url, String email) {
        return factory.uriString(url + "?email={email}")
            .build(email)
            .toString();

    }
}