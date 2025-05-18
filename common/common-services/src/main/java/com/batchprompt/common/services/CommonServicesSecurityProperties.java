package com.batchprompt.common.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "common.services.security")
@Data
public class CommonServicesSecurityProperties {
    private String serviceName = "common-services";
    private String rolesClaim;
    private List<String> allowedOrigins = new ArrayList<>();
}
