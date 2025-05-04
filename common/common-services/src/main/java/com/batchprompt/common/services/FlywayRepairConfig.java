package com.batchprompt.common.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayRepairConfig {
    @Value("${spring.flyway.repair:false}")
    private boolean repairOnStartup;
    
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            if (repairOnStartup) {
                flyway.repair();
            }
            flyway.migrate();
        };
    }
}