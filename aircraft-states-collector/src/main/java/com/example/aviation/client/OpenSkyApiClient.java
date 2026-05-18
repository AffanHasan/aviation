package com.example.aviation.client;

import com.example.aviation.config.OpenSkyProperties;
import com.example.aviation.domain.StateVector;
import com.example.aviation.domain.StateVectorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenSkyApiClient {

    private final RestTemplate restTemplate;
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final OpenSkyProperties properties;
    private final ObjectMapper objectMapper;

    public StateVectorResponse fetchStateVectors() {
        String url = properties.baseUrl() + "/states/all";
        HttpEntity<Void> request = new HttpEntity<>(buildHeaders());

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, request, String.class);

        logRateLimitHeaders(response);

        return parseResponse(response.getBody());
    }

    private void logRateLimitHeaders(ResponseEntity<String> response) {
        Optional.ofNullable(response.getHeaders().getFirst("X-Rate-Limit-Remaining"))
                .ifPresent(remaining -> log.info("OpenSky API credits remaining: {}", remaining));
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(obtainAccessToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private String obtainAccessToken() {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("opensky")
                .principal("opensky-client")
                .build();

        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);

        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            throw new IllegalStateException("Failed to obtain OAuth2 access token for OpenSky API");
        }

        return authorizedClient.getAccessToken().getTokenValue();
    }

    @SuppressWarnings("unchecked")
    private StateVectorResponse parseResponse(String body) {
        try {
            Map<String, Object> root = objectMapper.readValue(body, Map.class);
            Long time = ((Number) root.get("time")).longValue();

            List<List<Object>> rawStates = (List<List<Object>>) root.get("states");
            List<StateVector> states = null;
            if (rawStates != null) {
                states = rawStates.stream()
                        .map(StateVectorConverter::fromRawList)
                        .toList();
            }

            return new StateVectorResponse(time, states);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenSky API response", e);
        }
    }
}
