package com.batchprompt.files.core;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import lombok.extern.slf4j.Slf4j;

@Configuration
@ComponentScan
@EnableJpaRepositories
@EntityScan(basePackages = "com.batchprompt.files.core.model")
@Slf4j
public class FilesCoreAutoConfig {
    FilesCoreAutoConfig() {
        log.info("FilesCoreAutoConfig initialized");
    }
    
}
