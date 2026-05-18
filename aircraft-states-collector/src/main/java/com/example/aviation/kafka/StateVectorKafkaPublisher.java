package com.example.aviation.kafka;

import com.example.aviation.avro.StateVectorResponseAvro;
import com.example.aviation.domain.StateVectorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StateVectorKafkaPublisher {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final StateVectorAvroMapper mapper;
    private final String topic;
    private final AvroSerializer<StateVectorResponseAvro> serializer = new AvroSerializer<>();

    public StateVectorKafkaPublisher(
            KafkaTemplate<String, byte[]> kafkaTemplate,
            StateVectorAvroMapper mapper,
            @Value("${aviation.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.mapper = mapper;
        this.topic = topic;
    }

    public void publish(StateVectorResponse response) {
        try {
            StateVectorResponseAvro avro = mapper.toAvro(response);
            byte[] avroBytes = serializer.serialize(topic, avro);
            String key = String.valueOf(response.time());

            kafkaTemplate.send(topic, key, avroBytes)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish state vectors to Kafka topic {}", topic, ex);
                        } else {
                            log.info("Published state vectors to Kafka topic {} with key {} at offset {}",
                                    topic, key, result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to serialize and publish state vectors to Kafka", e);
            throw e;
        }
    }
}
