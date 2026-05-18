package com.example.aviation.unit;

import com.example.aviation.avro.StateVectorResponseAvro;
import com.example.aviation.domain.StateVector;
import com.example.aviation.domain.StateVectorResponse;
import com.example.aviation.kafka.StateVectorAvroMapper;
import com.example.aviation.kafka.StateVectorKafkaPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StateVectorKafkaPublisherTest {

    @Mock
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @Mock
    private StateVectorAvroMapper mapper;

    private StateVectorKafkaPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new StateVectorKafkaPublisher(kafkaTemplate, mapper, "aircraft.state.vectors");
    }

    @Test
    void publish_shouldSendOneMessageToTopic_withResponseTimeAsKey() {
        // Given
        StateVectorResponse response = new StateVectorResponse(
                1715974500L,
                List.of(new StateVector("3c6444", "DLH123  ", "Germany", 1715974495, 1715974498,
                        8.6821, 50.1109, 10668.0, false, 245.5, 95.0, 0.0,
                        List.of(12345), 10850.0, "2577", false, 0, 4))
        );

        StateVectorResponseAvro avro = new StateVectorResponseAvro();
        avro.setTime(1715974500L);
        when(mapper.toAvro(response)).thenReturn(avro);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // When
        publisher.publish(response);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> valueCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("aircraft.state.vectors");
        assertThat(keyCaptor.getValue()).isEqualTo("1715974500");
        assertThat(valueCaptor.getValue()).isNotNull();
    }

    @Test
    void publish_shouldSerializeToAvroBytes() {
        // Given
        StateVectorResponse response = new StateVectorResponse(1715974500L, List.of());
        StateVectorResponseAvro avro = new StateVectorResponseAvro();
        avro.setTime(1715974500L);
        when(mapper.toAvro(response)).thenReturn(avro);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // When
        publisher.publish(response);

        // Then
        ArgumentCaptor<byte[]> valueCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(kafkaTemplate).send(any(), any(), valueCaptor.capture());
        assertThat(valueCaptor.getValue()).isNotEmpty();
    }
}
