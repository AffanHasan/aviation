package com.example.aviation.service;

import com.example.aviation.client.OpenSkyApiClient;
import com.example.aviation.domain.StateVectorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StateVectorFetcherService {

    private final OpenSkyApiClient apiClient;

    public void fetchAndProcess() {
        try {
            StateVectorResponse response = apiClient.fetchStateVectors();
            int count = response.states() == null ? 0 : response.states().size();
            log.info("Fetched {} state vectors from OpenSky API (time={})", count, response.time());
        } catch (Exception e) {
            log.error("Failed to fetch state vectors from OpenSky API", e);
            throw e;
        }
    }
}
