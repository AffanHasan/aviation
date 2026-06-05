package aviation.kafka;

import aviation.domain.StateVectorResponse;
import com.example.aviation.avro.StateVectorResponseAvro;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;

@ApplicationScoped
public class StateVectorKafkaPublisher {

    @Inject
    @Channel("aircraft-state-vectors")
    Emitter<byte[]> emitter;

    @Inject
    StateVectorAvroMapper mapper;

    private final AvroSerializer<StateVectorResponseAvro> serializer = new AvroSerializer<>();

    public void publish(StateVectorResponse response) {
        try {
            StateVectorResponseAvro avro = mapper.toAvro(response);
            byte[] avroBytes = serializer.serialize("aircraft.state.vectors", avro);
            String key = String.valueOf(response.time());
            emitter.send(Message.of(avroBytes, Metadata.of(key)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize and publish state vectors to Kafka", e);
        }
    }
}
