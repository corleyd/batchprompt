package com.batchprompt.common.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing service-to-service authentication using Auth0 client credentials
 */
@Service
@Slf4j
public class ServiceAuthenticationService {

    private final String audience;
    private final String clientId;
    private final String domain;
    private final String rolesClaim;
    
    public ServiceAuthenticationService(
            @Value("${auth0.domain:}") String domain,
            @Value("${auth0.service.client-id:}") String clientId,
            @Value("${auth0.service.client-secret:}") String clientSecret,
            @Value("${auth0.audience:}") String audience,
            CommonServicesSecurityProperties securityProperties) {
        this.audience = audience;
        this.clientId = clientId;
        this.domain = domain;
        this.rolesClaim = securityProperties.getRolesClaim();
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
        List<String> roles = jwt.getClaimAsStringList(rolesClaim);
        return roles != null && roles.contains("admin");
    }

    public boolean canAccessUserData(Jwt jwt, String userId) {
        // Check if the user has the "admin" role or if the userId matches the JWT subject
        return isAdminUser(jwt) || jwt.getSubject().equals(userId) || isValidServiceJwt(jwt);
    }
    
}