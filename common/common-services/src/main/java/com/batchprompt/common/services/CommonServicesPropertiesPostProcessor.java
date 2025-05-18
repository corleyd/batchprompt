package com.batchprompt.common.services;

import java.io.IOException;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * An EnvironmentPostProcessor is called even earlier than ApplicationListeners
 * This will ensure the properties are loaded at the earliest possible point in the Spring Boot lifecycle
 */
public class CommonServicesPropertiesPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        try {
            Resource resource = new ClassPathResource("common-services-defaults.yml");
            if (resource.exists()) {
                System.out.println("CommonServicesPropertiesPostProcessor: Loading common-services-defaults.yml with HIGHEST precedence");
                List<PropertySource<?>> propertySourceList = loader.load("common-services-defaults", resource);
                for (PropertySource<?> propertySource : propertySourceList) {
                    // Add with highest precedence
                    environment.getPropertySources().addFirst(propertySource);
                    System.out.println("CommonServicesPropertiesPostProcessor: Added property source: " + propertySource.getName());
                }
                // Verify loading worked
                String testProp = environment.getProperty("common.services.security.allowed-origins[0]");
                if (testProp != null) {
                    System.out.println("CommonServicesPropertiesPostProcessor: Successfully loaded properties. Test value: " + testProp);
                } else {
                    System.err.println("CommonServicesPropertiesPostProcessor: WARNING - Properties may not be loaded correctly!");
                }
            } else {
                System.err.println("CommonServicesPropertiesPostProcessor: ERROR - common-services-defaults.yml not found in classpath!");
            }
        } catch (IOException e) {
            System.err.println("CommonServicesPropertiesPostProcessor: FATAL ERROR loading properties: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to load common-services-defaults.yml at earliest stage", e);
        }
    }

    @Override
    public int getOrder() {
        // Ensure this runs as early as possible
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
