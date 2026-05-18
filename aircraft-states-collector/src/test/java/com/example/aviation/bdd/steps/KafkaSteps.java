package com.example.aviation.bdd.steps;

import com.example.aviation.avro.StateVectorResponseAvro;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class KafkaSteps {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    private ConsumerRecord<String, byte[]> consumedRecord;

    @Given("Kafka is running")
    public void kafkaIsRunning() {
        assertThat(embeddedKafka).isNotNull();
    }

    @Then("the full state vector response should be published to the {string} topic")
    public void theFullStateVectorResponseShouldBePublishedToTheTopic(String topic) {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("cucumber-group", "true", embeddedKafka);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        Consumer<String, byte[]> consumer = new KafkaConsumer<>(consumerProps, new StringDeserializer(), new ByteArrayDeserializer());
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, topic);

        consumedRecord = KafkaTestUtils.getSingleRecord(consumer, topic, Duration.ofSeconds(10));
        assertThat(consumedRecord).isNotNull();
        assertThat(consumedRecord.topic()).isEqualTo(topic);

        consumer.close();
    }

    @And("the message should be Avro serialized with the response time as the key")
    public void theMessageShouldBeAvroSerializedWithTheResponseTimeAsTheKey() throws Exception {
        assertThat(consumedRecord).isNotNull();
        assertThat(consumedRecord.value()).isNotEmpty();

        DatumReader<StateVectorResponseAvro> datumReader = new SpecificDatumReader<>(StateVectorResponseAvro.class);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(consumedRecord.value(), null);
        StateVectorResponseAvro avro = datumReader.read(null, decoder);

        assertThat(avro.getTime()).isEqualTo(1715974500L);
        assertThat(consumedRecord.key()).isEqualTo("1715974500");
        assertThat(avro.getStates()).isNotEmpty();
        assertThat(avro.getStates().get(0).getIcao24().toString()).isEqualTo("3c6444");
    }
}
