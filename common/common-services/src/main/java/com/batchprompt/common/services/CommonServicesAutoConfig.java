package com.batchprompt.common.services;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;

import com.batchprompt.common.core.YamlPropertySourceFactory;

@Configuration
@ComponentScan
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@PropertySource(value = "classpath:common-services-defaults.yml", factory = YamlPropertySourceFactory.class)
public class CommonServicesAutoConfig {
    
}
