package com.batchprompt.common.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import com.auth0.client.auth.AuthAPI;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.TokenHolder;
import com.auth0.net.Response;
import com.auth0.net.TokenRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing service-to-service authentication using Auth0 client credentials
 */
@Service
@Slf4j
public class ClientAuthenticationService {

    private final AuthAPI authAPI;
    private final String audience;

    
    private TokenInfo cachedToken;

    public ClientAuthenticationService(
            @Value("${auth0.domain:}") String domain,
            @Value("${auth0.service.client-id:}") String clientId,
            @Value("${auth0.service.client-secret:}") String clientSecret,
            @Value("${auth0.audience:}") String audience) {
        this.audience = audience;
        // Use the non-deprecated constructor (remove "https://" from domain)
        String cleanDomain = domain.startsWith("https://") ? domain.substring(8) : domain;
        this.authAPI = AuthAPI.newBuilder(cleanDomain, clientId, clientSecret).build();
    }

    /**
     * Get a valid access token for service-to-service communication
     * @param serviceName The name of the service requesting the token (for caching)
     * @return A valid bearer token
     */
    public String getServiceToken() {
        // Check cache first
        if (cachedToken != null && !cachedToken.isExpired()) {
            log.debug("Using cached token");
            return "Bearer " + cachedToken.getAccessToken();
        }

        try {
            // Use Auth0 SDK to get token with client credentials grant
            TokenRequest request = authAPI.requestToken(audience);
            Response<TokenHolder> response = request.execute();
            TokenHolder holder = response.getBody();
            
            // Get token and handle caching
            String accessToken = holder.getAccessToken();
            long expiresIn = holder.getExpiresIn();
            
            // Calculate expiration time (with some buffer)
            long expiresAt = System.currentTimeMillis() + (expiresIn * 1000) - 60000; // -1 minute buffer
            TokenInfo tokenInfo = new TokenInfo(accessToken, expiresAt);
            
            // Cache the token
            cachedToken = tokenInfo;
            
            log.debug("Obtained new service token");
            return "Bearer " + accessToken;
        } catch (Auth0Exception e) {
            log.error("Error obtaining service token from Auth0", e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error obtaining service token", e);
            return null;
        }
    }

    
    /**
     * Get an effective token for the request. If user token is null, get a service token.
     * 
     * @param userToken The user's auth token or null
     * @return An effective token for the request
     */
    private String getEffectiveToken(String userToken) {
        if (userToken != null && !userToken.isEmpty()) {
            return userToken;
        }
        // Use service authentication when no user token is provided
        return getServiceToken();
    }    

    /**
     * Create HTTP headers with authentication token
     * 
     * @param token The authentication token
     * @return HTTP headers with properly formatted auth token
     */
    public HttpHeaders createAuthHeaders(String token) {
        String realToken = getEffectiveToken(token);

        HttpHeaders headers = new HttpHeaders();
        // Ensure token has the proper "Bearer " prefix
        if (realToken != null) {
            if (!realToken.startsWith("Bearer ")) {
                realToken = "Bearer " + realToken;
            }
            headers.set("Authorization", realToken);
        } else {
            log.warn("No valid token available for authentication");
        }
        return headers;
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