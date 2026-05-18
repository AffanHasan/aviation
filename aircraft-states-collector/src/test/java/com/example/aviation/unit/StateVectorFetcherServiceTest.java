package com.example.aviation.unit;

import com.example.aviation.client.OpenSkyApiClient;
import com.example.aviation.domain.StateVector;
import com.example.aviation.domain.StateVectorResponse;
import com.example.aviation.kafka.StateVectorKafkaPublisher;
import com.example.aviation.service.StateVectorFetcherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StateVectorFetcherServiceTest {

    @Mock
    private OpenSkyApiClient apiClient;

    @Mock
    private StateVectorKafkaPublisher kafkaPublisher;

    @InjectMocks
    private StateVectorFetcherService fetcherService;

    @Test
    void fetchAndProcess_shouldInvokeApiClient() {
        // Given
        StateVectorResponse response = new StateVectorResponse(
                1715974500L,
                List.of(new StateVector("3c6444", "DLH123  ", "Germany", 1715974495, 1715974498,
                        8.6821, 50.1109, 10668.0, false, 245.5, 95.0, 0.0,
                        List.of(12345), 10850.0, "2577", false, 0, 4))
        );
        when(apiClient.fetchStateVectors()).thenReturn(response);

        // When
        fetcherService.fetchAndProcess();

        // Then
        verify(apiClient).fetchStateVectors();
        verify(kafkaPublisher).publish(response);
    }

    @Test
    void fetchAndProcess_shouldHandleEmptyResponse() {
        // Given
        StateVectorResponse response = new StateVectorResponse(1715974500L, List.of());
        when(apiClient.fetchStateVectors()).thenReturn(response);

        // When
        fetcherService.fetchAndProcess();

        // Then
        verify(apiClient).fetchStateVectors();
        verify(kafkaPublisher).publish(response);
    }
}
