package aviation;

import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import aviation.actor.AircraftStatesManagerBehavior;
import aviation.actor.AircraftStatesManagerCommand;
import aviation.actor.KafkaConsumerBehavior;

public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final String SYSTEM_NAME = "aircraft-state-event-sourcing";
    private static final String KAFKA_CONSUMER_ACTOR_NAME = "kafka-consumer";

    private Main() {
        // entry point class
    }

    public static void main(final String[] args) {
        final Config config = ConfigFactory.load();
        final String bootstrapServers = config.getString("kafka.bootstrap-servers");
        final String topic = config.getString("kafka.topic");

        final ActorSystem<AircraftStatesManagerCommand> system =
                ActorSystem.create(AircraftStatesManagerBehavior.create(), SYSTEM_NAME);

        system.systemActorOf(
                KafkaConsumerBehavior.create(system, bootstrapServers, topic),
                KAFKA_CONSUMER_ACTOR_NAME,
                Props.empty());

        LOG.info("Aircraft state event sourcing started. Consuming from topic '{}' at {}.", topic, bootstrapServers);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutting down actor system...");
            system.terminate();
            system.getWhenTerminated().toCompletableFuture().join();
            LOG.info("Actor system terminated.");
        }));
    }
}
