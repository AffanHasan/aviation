package aviation.actor;

import java.io.Serializable;

public sealed interface KafkaConsumerProtocol extends Serializable {

    record Stop() implements KafkaConsumerProtocol {
        private static final long serialVersionUID = 1L;
    }
}
