package com.batchprompt.waitlist.core;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackages = "com.batchprompt.waitlist.core")
@EntityScan(basePackages = "com.batchprompt.waitlist.core.model")
@EnableJpaRepositories(basePackages = "com.batchprompt.waitlist.core.repository")
public class WaitlistCoreAutoConfig {
}