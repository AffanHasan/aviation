package com.example.aviation.integration;

import com.example.aviation.avro.StateVectorResponseAvro;
import com.example.aviation.client.OpenSkyApiClient;
import com.example.aviation.domain.StateVector;
import com.example.aviation.domain.StateVectorResponse;
import com.example.aviation.kafka.AvroSerializer;
import com.example.aviation.service.StateVectorFetcherService;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = "aircraft.state.vectors")
class StateVectorKafkaIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private StateVectorFetcherService fetcherService;

    @MockBean
    private OpenSkyApiClient apiClient;

    @Test
    void fetchAndProcess_shouldPublishAvroMessageToTopic() throws Exception {
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

        // Then — consume and verify
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafka);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        Consumer<String, byte[]> consumer = new KafkaConsumer<>(consumerProps, new StringDeserializer(), new ByteArrayDeserializer());
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "aircraft.state.vectors");

        ConsumerRecord<String, byte[]> record = KafkaTestUtils.getSingleRecord(consumer, "aircraft.state.vectors", Duration.ofSeconds(5));

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("1715974500");

        // Deserialize Avro
        DatumReader<StateVectorResponseAvro> datumReader = new SpecificDatumReader<>(StateVectorResponseAvro.class);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(record.value(), null);
        StateVectorResponseAvro avro = datumReader.read(null, decoder);

        assertThat(avro.getTime()).isEqualTo(1715974500L);
        assertThat(avro.getStates()).hasSize(1);
        assertThat(avro.getStates().get(0).getIcao24().toString()).isEqualTo("3c6444");

        consumer.close();
    }
}
