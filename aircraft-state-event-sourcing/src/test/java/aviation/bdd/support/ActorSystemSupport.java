package aviation.bdd.support;

import java.util.concurrent.TimeUnit;

import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;

import aviation.actor.AircraftStatesManagerBehavior;
import aviation.actor.AircraftStatesManagerCommand;
import aviation.actor.KafkaConsumerBehavior;

public final class ActorSystemSupport {

    private static final String SYSTEM_NAME = "aircraft-state-event-sourcing-test";
    private static final String KAFKA_CONSUMER_ACTOR_NAME = "kafka-consumer";
    private static final int TERMINATION_TIMEOUT_SECONDS = 10;

    private ActorSystem<AircraftStatesManagerCommand> system;

    public void start(final TestcontainersSupport infrastructure) {
        system = ActorSystem.create(AircraftStatesManagerBehavior.create(), SYSTEM_NAME);
        system.systemActorOf(
                KafkaConsumerBehavior.create(system, infrastructure.kafkaBootstrapServers(), "aircraft.state.vectors"),
                KAFKA_CONSUMER_ACTOR_NAME,
                Props.empty());
    }

    public void terminate() throws Exception {
        if (system != null) {
            system.terminate();
            system.getWhenTerminated().toCompletableFuture().get(TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    public ActorSystem<AircraftStatesManagerCommand> system() {
        return system;
    }

    public boolean isRunning() {
        return system != null && !system.getWhenTerminated().toCompletableFuture().isDone();
    }
}
