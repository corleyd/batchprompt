package com.batchprompt.common.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;


@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "auth0.audience") // Only activate when auth0.audience property is set
public class SecurityConfig {

    @Value("${auth0.audience}")
    private String audience;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuer;

    @Autowired
    private CommonServicesSecurityProperties securityProperties;

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> {
            CorsConfigurationSource corsConfigurationSource = request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));

                /* 
                 * The following is necessary. Without it, the STOMP client will complain about:
                 * 
                 * The value of the 'Access-Control-Allow-Credentials' header in the response is '' which must be 'true' when the request's credentials mode is 'include'. The credentials mode of requests initiated by the XMLHttpRequest is controlled by the withCredentials attribute.
                 * 
                 * When we set that, we're not allowed to set the allowed origins to "*", so we have to specify the exact origins.
                 */

                config.setAllowCredentials(true);
                config.setAllowedOrigins(securityProperties.getAllowedOrigins());
                return config;
            };
            cors.configurationSource(corsConfigurationSource);
        });
        
        // Disable CSRF protection for API endpoints (using JWT tokens instead)
        http.csrf(csrf -> csrf.disable());
        
        http.authorizeHttpRequests(authz -> authz
            .requestMatchers("/api/public/**", "/test/**", "/ws/**", "/api/model-management/models", "/api/model-management/providers", "/api/model-management/providers/enabled", "/api/model-management/credit-usage").permitAll()
            .requestMatchers(RegexRequestMatcher.regexMatcher("^/api/[a-z0-9\\-]+/public/.+$")).permitAll()
            .requestMatchers(RegexRequestMatcher.regexMatcher("^/api/files/[a-f0-9\\-]+/download/[a-f0-9\\-]+$")).permitAll()
            .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder())));
        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromOidcIssuerLocation(issuer);

        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audience);
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

        jwtDecoder.setJwtValidator(withAudience);

        return jwtDecoder;
    }
}
