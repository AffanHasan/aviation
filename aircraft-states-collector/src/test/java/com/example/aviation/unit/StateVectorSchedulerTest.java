package com.example.aviation.unit;

import com.example.aviation.scheduler.StateVectorScheduler;
import com.example.aviation.service.StateVectorFetcherService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "opensky.interval-ms=1000")
@ExtendWith(MockitoExtension.class)
class StateVectorSchedulerTest {

    @SpyBean
    private StateVectorFetcherService fetcherService;

    @Test
    void schedulerJob_shouldTriggerService_every5Minutes() {
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(fetcherService, atLeast(1)).fetchAndProcess());
    }
}
