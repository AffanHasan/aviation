package aviation.kafka;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.aviation.avro.StateVectorResponseAvro;

public final class AvroDeserializer {

    private static final Logger LOG = LoggerFactory.getLogger(AvroDeserializer.class);

    private AvroDeserializer() {
        // utility class
    }

    public static StateVectorResponseAvro deserialize(final byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        final DatumReader<StateVectorResponseAvro> datumReader = new SpecificDatumReader<>(StateVectorResponseAvro.class);
        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            final BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(in, null);
            return datumReader.read(null, decoder);
        } catch (IOException e) {
            LOG.error("Failed to deserialize StateVectorResponseAvro", e);
            return null;
        }
    }
}
