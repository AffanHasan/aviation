package aviation.actor;

import java.io.Serializable;
import java.util.Optional;

import org.apache.pekko.actor.typed.ActorRef;

import com.example.aviation.avro.StateVectorAvro;

import aviation.domain.AircraftState;

public sealed interface AircraftStatesManagerCommand extends Serializable {

    record ProcessStateVector(StateVectorAvro stateVector) implements AircraftStatesManagerCommand {
        private static final long serialVersionUID = 1L;
    }

    record GetAircraftState(String icao24, ActorRef<Optional<AircraftState>> replyTo) implements AircraftStatesManagerCommand {
        private static final long serialVersionUID = 1L;
    }
}
