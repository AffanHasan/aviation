package com.example.aviation;

import com.example.aviation.config.OpenSkyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(OpenSkyProperties.class)
public class AviationSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AviationSchedulerApplication.class, args);
    }
}
