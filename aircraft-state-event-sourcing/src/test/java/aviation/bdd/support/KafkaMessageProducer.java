package aviation.bdd.support;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import com.example.aviation.avro.StateVectorAvro;
import com.example.aviation.avro.StateVectorResponseAvro;

public final class KafkaMessageProducer {

    private static final int DEFAULT_PARTITIONS = 1;
    private static final short DEFAULT_REPLICATION_FACTOR = 1;
    private static final int PRODUCE_TIMEOUT_SECONDS = 5;

    private final String bootstrapServers;

    public KafkaMessageProducer(final String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public void createTopic(final String topic) throws Exception {
        final Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        try (AdminClient admin = AdminClient.create(props)) {
            admin.createTopics(List.of(new NewTopic(topic, DEFAULT_PARTITIONS, DEFAULT_REPLICATION_FACTOR)))
                    .all()
                    .get(PRODUCE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    public void produceStateVectorResponse(final String topic, final StateVectorResponseAvro response) throws Exception {
        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

        try (KafkaProducer<String, byte[]> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(topic, String.valueOf(response.getTime()), serialize(response)))
                    .get(PRODUCE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    public StateVectorAvro buildStateVector(final String icao24,
                                            final String callsign,
                                            final String originCountry,
                                            final int timePosition,
                                            final double latitude,
                                            final double longitude) {
        final StateVectorAvro sv = new StateVectorAvro();
        sv.setIcao24(icao24);
        sv.setCallsign(callsign);
        sv.setOriginCountry(originCountry);
        sv.setTimePosition(timePosition);
        sv.setLastContact(timePosition + 3);
        sv.setLongitude(longitude);
        sv.setLatitude(latitude);
        sv.setBaroAltitude(10668.0);
        sv.setOnGround(false);
        sv.setVelocity(245.5);
        sv.setTrueTrack(95.0);
        sv.setVerticalRate(0.0);
        sv.setSensors(List.of(12345, 67890));
        sv.setGeoAltitude(10850.0);
        sv.setSquawk("2577");
        sv.setSpi(false);
        sv.setPositionSource(0);
        sv.setCategory(4);
        return sv;
    }

    private byte[] serialize(final StateVectorResponseAvro response) throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        final DatumWriter<StateVectorResponseAvro> writer = new SpecificDatumWriter<>(StateVectorResponseAvro.class);
        writer.write(response, encoder);
        encoder.flush();
        return out.toByteArray();
    }
}
