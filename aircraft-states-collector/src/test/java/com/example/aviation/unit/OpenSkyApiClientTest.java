package com.example.aviation.unit;

import com.example.aviation.client.OpenSkyApiClient;
import com.example.aviation.config.OpenSkyProperties;
import com.example.aviation.domain.StateVectorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenSkyApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @Mock
    private OpenSkyProperties properties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OpenSkyApiClient client;

    @BeforeEach
    void setUp() {
        when(properties.baseUrl()).thenReturn("http://localhost:8089");
        client = new OpenSkyApiClient(restTemplate, authorizedClientManager, properties, objectMapper);
    }

    @Test
    void fetchStateVectors_shouldReturnParsedResponse_onSuccess() {
        // Given
        String jsonResponse = """
                {"time":1715974500,"states":[["3c6444","DLH123  ","Germany",1715974495,1715974498,8.6821,50.1109,10668.0,false,245.5,95.0,0.0,null,10850.0,"2577",false,0,4]]}
                """;

        mockAuthorizedClient();
        when(restTemplate.exchange(
                eq("http://localhost:8089/states/all"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(jsonResponse));

        // When
        StateVectorResponse response = client.fetchStateVectors();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.time()).isEqualTo(1715974500L);
        assertThat(response.states()).hasSize(1);
        assertThat(response.states().get(0).icao24()).isEqualTo("3c6444");
    }

    @Test
    void fetchStateVectors_shouldIncludeBearerTokenInHeader() {
        // Given
        mockAuthorizedClient();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"time\":1,\"states\":null}"));

        // When
        client.fetchStateVectors();

        // Then
        verify(restTemplate).exchange(
                anyString(),
                eq(HttpMethod.GET),
                argThat(entity -> {
                    String auth = entity.getHeaders().getFirst("Authorization");
                    return auth != null && auth.equals("Bearer test-access-token");
                }),
                eq(String.class)
        );
    }

    @Test
    void fetchStateVectors_shouldThrowException_onHttpError() {
        // Given
        mockAuthorizedClient();
        doThrow(new RuntimeException("Connection refused"))
                .when(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));

        // Then
        assertThatThrownBy(() -> client.fetchStateVectors())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Connection refused");
    }

    private void mockAuthorizedClient() {
        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-access-token",
                Instant.now(),
                Instant.now().plusSeconds(1800)
        );
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(authorizedClientManager.authorize(any())).thenReturn(authorizedClient);
    }
}
