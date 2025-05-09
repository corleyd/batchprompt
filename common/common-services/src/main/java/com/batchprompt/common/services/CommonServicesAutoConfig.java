package com.batchprompt.common.services;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.batchprompt.common.core.YamlPropertySourceFactory;

@Configuration
@ComponentScan
@PropertySource(value = "classpath:common-services-defaults.yml", factory = YamlPropertySourceFactory.class)
public class CommonServicesAutoConfig {
    
}
