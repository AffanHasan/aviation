package aviation.kafka;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;

import java.io.ByteArrayOutputStream;

public class AvroSerializer<T extends SpecificRecordBase> {

    public byte[] serialize(String topic, T data) {
        if (data == null) {
            return null;
        }
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
            DatumWriter<T> datumWriter = new SpecificDatumWriter<>(data.getSchema());
            datumWriter.write(data, encoder);
            encoder.flush();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize Avro record", e);
        }
    }
}
