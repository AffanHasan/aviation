package aviation.actor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.apache.pekko.persistence.typed.PersistenceId;

import com.example.aviation.avro.StateVectorAvro;

import aviation.domain.AircraftState;

public class AircraftStatesManagerBehavior extends AbstractBehavior<AircraftStatesManagerCommand> {

    public static Behavior<AircraftStatesManagerCommand> create() {
        return Behaviors.setup(AircraftStatesManagerBehavior::new);
    }

    private static final String PERSISTENCE_ID_PREFIX = "aircraft-";
    private static final String ACTOR_NAME_PREFIX = "aircraft-";
    private static final String ICAO24_EMPTY = "";

    private final Map<String, ActorRef<AircraftCommand>> aircraftActors = new HashMap<>();

    private AircraftStatesManagerBehavior(final ActorContext<AircraftStatesManagerCommand> context) {
        super(context);
    }

    @Override
    public Receive<AircraftStatesManagerCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(AircraftStatesManagerCommand.ProcessStateVector.class, this::onProcessStateVector)
                .onMessage(AircraftStatesManagerCommand.GetAircraftState.class, this::onGetAircraftState)
                .build();
    }

    private Behavior<AircraftStatesManagerCommand> onProcessStateVector(final AircraftStatesManagerCommand.ProcessStateVector cmd) {
        final StateVectorAvro sv = cmd.stateVector();
        final String icao24 = sv.getIcao24().toString();
        if (icao24 == null || ICAO24_EMPTY.equals(icao24.trim())) {
            return this;
        }
        final ActorRef<AircraftCommand> actor = aircraftActors.computeIfAbsent(icao24,
                id -> getContext().spawn(
                        AircraftStateBehavior.create(PersistenceId.ofUniqueId(PERSISTENCE_ID_PREFIX + id)),
                        sanitizeActorName(ACTOR_NAME_PREFIX + id)));
        actor.tell(new AircraftCommand.ProcessStateVector(sv));
        return this;
    }

    private Behavior<AircraftStatesManagerCommand> onGetAircraftState(final AircraftStatesManagerCommand.GetAircraftState cmd) {
        final ActorRef<AircraftCommand> actor = aircraftActors.get(cmd.icao24());
        if (actor != null) {
            actor.tell(new AircraftCommand.GetAircraftState(cmd.replyTo()));
        } else {
            cmd.replyTo().tell(Optional.empty());
        }
        return this;
    }

    private static String sanitizeActorName(final String name) {
        final StringBuilder sb = new StringBuilder(name.length());
        for (final char c : name.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }
}
