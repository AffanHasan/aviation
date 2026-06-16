package aviation.actor;

import java.util.concurrent.CompletionStage;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.pekko.Done;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.PostStop;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.apache.pekko.kafka.ConsumerSettings;
import org.apache.pekko.kafka.Subscriptions;
import org.apache.pekko.kafka.javadsl.Consumer;
import org.apache.pekko.stream.javadsl.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.aviation.avro.StateVectorAvro;
import com.example.aviation.avro.StateVectorResponseAvro;

import aviation.kafka.AvroDeserializer;

public class KafkaConsumerBehavior extends AbstractBehavior<KafkaConsumerProtocol> {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaConsumerBehavior.class);
    private static final String CONSUMER_GROUP_ID = "aircraft-state-event-sourcing";
    private static final String AUTO_OFFSET_RESET = "earliest";
    private static final String AUTO_COMMIT_DISABLED = "false";

    public static Behavior<KafkaConsumerProtocol> create(final ActorRef<AircraftStatesManagerCommand> manager,
                                                         final String bootstrapServers,
                                                         final String topic) {
        return Behaviors.setup(context -> new KafkaConsumerBehavior(context, manager, bootstrapServers, topic));
    }

    private final ActorRef<AircraftStatesManagerCommand> manager;
    private final String bootstrapServers;
    private final String topic;
    private CompletionStage<Done> streamCompletion;

    private KafkaConsumerBehavior(final ActorContext<KafkaConsumerProtocol> context,
                                  final ActorRef<AircraftStatesManagerCommand> manager,
                                  final String bootstrapServers,
                                  final String topic) {
        super(context);
        this.manager = manager;
        this.bootstrapServers = bootstrapServers;
        this.topic = topic;
        startConsumer();
    }

    @Override
    public Receive<KafkaConsumerProtocol> createReceive() {
        return newReceiveBuilder()
                .onMessage(KafkaConsumerProtocol.Stop.class, msg -> Behaviors.stopped())
                .onSignal(PostStop.class, signal -> {
                    if (streamCompletion != null) {
                        streamCompletion.toCompletableFuture().cancel(true);
                    }
                    return this;
                })
                .build();
    }

    private void startConsumer() {
        final ConsumerSettings<String, byte[]> consumerSettings =
                ConsumerSettings.create(getContext().getSystem().classicSystem(),
                                new StringDeserializer(), new ByteArrayDeserializer())
                        .withBootstrapServers(bootstrapServers)
                        .withGroupId(CONSUMER_GROUP_ID)
                        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, AUTO_OFFSET_RESET)
                        .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, AUTO_COMMIT_DISABLED);

        streamCompletion = Consumer
                .plainSource(consumerSettings, Subscriptions.topics(topic))
                .flatMapConcat(record -> {
                    final StateVectorResponseAvro response = AvroDeserializer.deserialize(record.value());
                    if (response == null || response.getStates() == null) {
                        return Source.empty();
                    }
                    return Source.from(response.getStates());
                })
                .runForeach(this::dispatch, getContext().getSystem().classicSystem());

        streamCompletion.whenComplete((done, ex) -> {
            if (ex != null) {
                LOG.error("Kafka consumer stream failed", ex);
            }
        });
    }

    private void dispatch(final StateVectorAvro stateVector) {
        manager.tell(new AircraftStatesManagerCommand.ProcessStateVector(stateVector));
    }
}
