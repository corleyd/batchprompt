package com.batchprompt.users.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.client.auth.AuthAPI;
import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.mgmt.filter.UserFilter;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.TokenHolder;
import com.auth0.json.mgmt.users.User;
import com.auth0.net.TokenRequest;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class Auth0ManagementService {

    private final String domain;
    private final String clientId;
    private final String clientSecret;
    private final String audience;
    private TokenInfo managementTokenInfo;
    
    // Cache for user profiles - userId -> profile
    private final Map<String, UserProfileInfo> userProfileCache = new ConcurrentHashMap<>();
    
    public Auth0ManagementService(
            @Value("${auth0.admin-domain}") String domain,
            @Value("${auth0.service.client-id}") String clientId,
            @Value("${auth0.service.client-secret}") String clientSecret) {
        this.domain = domain;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.audience = "https://" + domain + "/api/v2/";
    }
    
    /**
     * Get user profile information from Auth0
     * @param userId The Auth0 user ID (sub claim from the JWT)
     * @return The Auth0 user profile or null if not found
     */
    public User getUserProfile(String userId) {
        try {
            // Check cache first
            UserProfileInfo cachedProfile = userProfileCache.get(userId);
            if (cachedProfile != null && !cachedProfile.isExpired()) {
                log.debug("Using cached user profile for user {}", userId);
                return cachedProfile.getProfile();
            }
            
            // Get or refresh management API token
            String accessToken = getManagementApiToken();
            if (accessToken == null) {
                log.error("Failed to obtain Management API token");
                return null;
            }
            
            // Initialize management API with token
            ManagementAPI mgmt = ManagementAPI.newBuilder(domain, accessToken)
                                              .build();
            
            // Get user profile by ID
            User profile = mgmt.users().get(userId, new UserFilter()).execute().getBody();
            
            // Cache the profile (with 5-minute expiration)
            long expiresAt = System.currentTimeMillis() + (5 * 60 * 1000);
            userProfileCache.put(userId, new UserProfileInfo(profile, expiresAt));
            
            return profile;
        } catch (Auth0Exception e) {
            log.error("Error fetching user profile from Auth0 for user {}", userId, e);
            return null;
        }
    }
    
    /**
     * Get a valid Management API token
     * @return The Management API access token
     */
    private String getManagementApiToken() {
        // Check if we have a valid token
        if (managementTokenInfo != null && !managementTokenInfo.isExpired()) {
            return managementTokenInfo.getAccessToken();
        }
        
        try {
            // Get a new token
            AuthAPI authAPI = AuthAPI.newBuilder(domain, clientId, clientSecret).build();
            TokenRequest request = authAPI.requestToken(audience);
            TokenHolder holder = request.execute().getBody();
            
            String accessToken = holder.getAccessToken();
            long expiresIn = holder.getExpiresIn();
            
            // Calculate expiration time (with some buffer)
            long expiresAt = System.currentTimeMillis() + (expiresIn * 1000) - 60000; // -1 minute buffer
            managementTokenInfo = new TokenInfo(accessToken, expiresAt);
            
            log.debug("Obtained new Auth0 Management API token");
            return accessToken;
        } catch (Auth0Exception e) {
            log.error("Error obtaining Auth0 Management API token", e);
            return null;
        }
    }
    
    /**
     * Class representing token information including expiration
     */
    private static class TokenInfo {
        private final String accessToken;
        private final long expiresAt;
        
        public TokenInfo(String accessToken, long expiresAt) {
            this.accessToken = accessToken;
            this.expiresAt = expiresAt;
        }
        
        public String getAccessToken() {
            return accessToken;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
    
    /**
     * Class representing user profile information including expiration
     */
    private static class UserProfileInfo {
        private final User profile;
        private final long expiresAt;
        
        public UserProfileInfo(User profile, long expiresAt) {
            this.profile = profile;
            this.expiresAt = expiresAt;
        }
        
        public User getProfile() {
            return profile;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}