package com.example.aviation.scheduler;

import com.example.aviation.service.StateVectorFetcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StateVectorScheduler {

    private final StateVectorFetcherService fetcherService;

    @Scheduled(fixedRateString = "${opensky.interval-ms:300000}")
    public void scheduledFetch() {
        log.debug("Scheduler triggered: fetching state vectors");
        fetcherService.fetchAndProcess();
    }
}
