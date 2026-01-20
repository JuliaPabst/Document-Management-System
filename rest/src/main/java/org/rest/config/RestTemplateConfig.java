package org.rest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configures RestTemplate for HTTP communication with external services (e.g., search service)
 */
@Configuration
public class RestTemplateConfig {

    @Value("${search.service.url}")
    private String searchServiceUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public String searchServiceUrl() {
        return searchServiceUrl;
    }
}
