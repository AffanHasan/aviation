package com.example.aviation.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "opensky")
public record OpenSkyProperties(
        @NotBlank String baseUrl,
        @NotNull Long intervalMs,
        @NotNull Timeouts timeouts
) {
    public record Timeouts(@NotNull Integer connectMs, @NotNull Integer readMs) {}
}
