package com.batchprompt.users.api.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"com.batchprompt.users.core"})
public class UsersApiConfig {
    // Configuration settings for the user service API
}