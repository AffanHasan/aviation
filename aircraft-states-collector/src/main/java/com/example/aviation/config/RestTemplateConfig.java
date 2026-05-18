package com.example.aviation.config;

import com.example.aviation.client.OpenSkyResponseErrorHandler;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, OpenSkyProperties properties) {
        return builder
                .connectTimeout(Duration.ofMillis(properties.timeouts().connectMs()))
                .readTimeout(Duration.ofMillis(properties.timeouts().readMs()))
                .errorHandler(new OpenSkyResponseErrorHandler())
                .build();
    }
}
