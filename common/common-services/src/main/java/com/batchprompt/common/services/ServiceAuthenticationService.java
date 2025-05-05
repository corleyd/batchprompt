package com.batchprompt.common.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.auth0.client.auth.AuthAPI;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.TokenHolder;
import com.auth0.net.TokenRequest;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing service-to-service authentication using Auth0 client credentials
 */
@Service
@Slf4j
public class ServiceAuthenticationService {

    private final AuthAPI authAPI;
    private final String audience;
    private final String clientId;
    private final String domain;
    
    // Cache for service tokens - service name -> token info
    private final Map<String, TokenInfo> tokenCache = new ConcurrentHashMap<>();

    public ServiceAuthenticationService(
            @Value("${auth0.domain:}") String domain,
            @Value("${auth0.service.client-id:}") String clientId,
            @Value("${auth0.service.client-secret:}") String clientSecret,
            @Value("${auth0.audience:}") String audience) {
        this.authAPI = new AuthAPI(domain, clientId, clientSecret);
        this.audience = audience;
        this.clientId = clientId;
        this.domain = domain;
    }

    /**
     * Get a valid access token for service-to-service communication
     * @param serviceName The name of the service requesting the token (for caching)
     * @return A valid bearer token
     */
    public String getServiceToken(String serviceName) {
        // Check cache first
        TokenInfo cachedToken = tokenCache.get(serviceName);
        if (cachedToken != null && !cachedToken.isExpired()) {
            log.debug("Using cached token for service {}", serviceName);
            return "Bearer " + cachedToken.getAccessToken();
        }

        try {
            // Use Auth0 SDK to get token with client credentials grant
            TokenRequest request = authAPI.requestToken(audience);
            TokenHolder holder = request.execute();
            
            // Get token and handle caching
            String accessToken = holder.getAccessToken();
            long expiresIn = holder.getExpiresIn();
            
            // Calculate expiration time (with some buffer)
            long expiresAt = System.currentTimeMillis() + (expiresIn * 1000) - 60000; // -1 minute buffer
            TokenInfo tokenInfo = new TokenInfo(accessToken, expiresAt);
            
            // Cache the token
            tokenCache.put(serviceName, tokenInfo);
            
            log.debug("Obtained new service token for {}", serviceName);
            return "Bearer " + accessToken;
        } catch (Auth0Exception e) {
            log.error("Error obtaining service token from Auth0", e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error obtaining service token", e);
            return null;
        }
    }

    public boolean isValidServiceJwt(Jwt jwt) {
        /*
         * Verify that the azt matches the client id of the service account
         * and that the iss matches the issuer of the service account
         */
        String azp = jwt.getClaimAsString("azp");
        String iss = jwt.getClaimAsString("iss");
        List<String> audienceList = jwt.getClaimAsStringList("aud");

        return azp != null && azp.equals(clientId) &&
            iss != null && iss.contains(domain) &&
            audienceList != null && audienceList.contains(audience);
    }

    public boolean isAdminUser(Jwt jwt) {
        // Check if the user has the "admin" role
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("admin");
    }

    public boolean canAccessUserData(Jwt jwt, String userId) {
        // Check if the user has the "admin" role or if the userId matches the JWT subject
        return isAdminUser(jwt) || jwt.getSubject().equals(userId);
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
}