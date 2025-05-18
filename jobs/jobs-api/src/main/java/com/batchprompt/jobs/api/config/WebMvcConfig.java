package com.batchprompt.jobs.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Configuration for the web application features including template engine for WebSocket test page.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Configure template resolver for Thymeleaf
     */
    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setPrefix("classpath:/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCacheable(false); // Set to true in production
        return templateResolver;
    }

    /**
     * Configure Spring template engine
     */
    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver());
        return templateEngine;
    }

    /**
     * Add view controllers for simple paths
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // This maps the /wstest URL to the wstest template
        registry.addViewController("/wstest").setViewName("wstest");
    }
}
