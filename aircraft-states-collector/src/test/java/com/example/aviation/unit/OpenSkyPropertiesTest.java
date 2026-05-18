package com.example.aviation.unit;

import com.example.aviation.config.OpenSkyProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OpenSkyPropertiesTest {

    @Autowired
    private OpenSkyProperties properties;

    @Test
    void shouldBindBaseUrl() {
        assertThat(properties.baseUrl()).isEqualTo("http://localhost:8089");
    }

    @Test
    void shouldBindIntervalMs() {
        assertThat(properties.intervalMs()).isEqualTo(3600000L);
    }

    @Test
    void shouldBindTimeouts() {
        assertThat(properties.timeouts()).isNotNull();
        assertThat(properties.timeouts().connectMs()).isEqualTo(2000);
        assertThat(properties.timeouts().readMs()).isEqualTo(5000);
    }
}
