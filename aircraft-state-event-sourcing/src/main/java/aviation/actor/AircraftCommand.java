package aviation.actor;

import java.io.Serializable;
import java.util.Optional;

import org.apache.pekko.actor.typed.ActorRef;

import com.example.aviation.avro.StateVectorAvro;

import aviation.domain.AircraftState;

public sealed interface AircraftCommand extends Serializable {

    record ProcessStateVector(StateVectorAvro stateVector) implements AircraftCommand {
        private static final long serialVersionUID = 1L;
    }

    record GetAircraftState(ActorRef<Optional<AircraftState>> replyTo) implements AircraftCommand {
        private static final long serialVersionUID = 1L;
    }
}
