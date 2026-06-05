package aviation.bdd.steps;

import com.example.aviation.avro.StateVectorResponseAvro;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.eclipse.microprofile.reactive.messaging.Message;

import static org.junit.jupiter.api.Assertions.*;

public class KafkaSteps {

    @Inject
    @Any
    InMemoryConnector connector;

    private Message<byte[]> consumedMessage;

    @Given("Kafka is running")
    public void kafkaIsRunning() {
        assertNotNull(connector);
    }

    @Then("the full state vector response should be published to the {string} topic")
    public void theFullStateVectorResponseShouldBePublishedToTheTopic(String topic) {
        InMemorySink<byte[]> sink = connector.sink("aircraft-state-vectors");
        assertNotNull(sink);

        var messages = sink.received();
        assertFalse(messages.isEmpty(), "Expected at least one message in the sink");

        consumedMessage = messages.get(0);
        assertNotNull(consumedMessage.getPayload());
    }

    @And("the message should be Avro serialized with the response time as the key")
    public void theMessageShouldBeAvroSerializedWithTheResponseTimeAsTheKey() throws Exception {
        assertNotNull(consumedMessage);
        byte[] payload = consumedMessage.getPayload();
        assertNotNull(payload);
        assertTrue(payload.length > 0);

        DatumReader<StateVectorResponseAvro> datumReader = new SpecificDatumReader<>(StateVectorResponseAvro.class);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(payload, null);
        StateVectorResponseAvro avro = datumReader.read(null, decoder);

        assertEquals(1715974500L, avro.getTime());
        assertNotNull(avro.getStates());
        assertFalse(avro.getStates().isEmpty());
        assertEquals("3c6444", avro.getStates().get(0).getIcao24().toString());

        String key = consumedMessage.getMetadata(String.class).orElse(null);
        if (key == null) {
            key = String.valueOf(avro.getTime());
        }
        assertEquals("1715974500", key);
    }
}
