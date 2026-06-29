package aviation.actor;

import java.util.Optional;

import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.persistence.typed.PersistenceId;
import org.apache.pekko.persistence.typed.javadsl.CommandHandler;
import org.apache.pekko.persistence.typed.javadsl.Effect;
import org.apache.pekko.persistence.typed.javadsl.EventHandler;
import org.apache.pekko.persistence.typed.javadsl.EventSourcedBehavior;

import com.example.aviation.avro.StateVectorAvro;

import aviation.domain.AircraftAttributes;
import aviation.domain.AircraftState;
import aviation.domain.AircraftStateEvent;

public class AircraftStateBehavior extends EventSourcedBehavior<AircraftStateCommand, AircraftStateEvent, AircraftState> {

    public static Behavior<AircraftStateCommand> create(final PersistenceId persistenceId) {
        return new AircraftStateBehavior(persistenceId);
    }

    private AircraftStateBehavior(final PersistenceId persistenceId) {
        super(persistenceId);
    }

    @Override
    public AircraftState emptyState() {
        return AircraftState.empty();
    }

    @Override
    public CommandHandler<AircraftStateCommand, AircraftStateEvent, AircraftState> commandHandler() {
        return newCommandHandlerBuilder()
                .forAnyState()
                .onCommand(AircraftStateCommand.ProcessStateVector.class, this::onProcessStateVector)
                .onCommand(AircraftStateCommand.GetAircraftState.class, this::onGetAircraftState)
                .build();
    }

    @Override
    public EventHandler<AircraftState, AircraftStateEvent> eventHandler() {
        return newEventHandlerBuilder()
                .forAnyState()
                .onEvent(AircraftStateEvent.class, this::applyEvent)
                .build();
    }

    private Effect<AircraftStateEvent, AircraftState> onProcessStateVector(
            final AircraftState state,
            final AircraftStateCommand.ProcessStateVector cmd) {
        final StateVectorAvro sv = cmd.stateVector();
        final AircraftAttributes attributes = new AircraftAttributes(
                sv.getIcao24().toString(),
                sv.getCallsign() != null ? sv.getCallsign().toString() : null,
                sv.getOriginCountry().toString(),
                sv.getTimePosition(),
                sv.getLastContact(),
                sv.getLongitude(),
                sv.getLatitude(),
                sv.getBaroAltitude(),
                sv.getOnGround(),
                sv.getVelocity(),
                sv.getTrueTrack(),
                sv.getVerticalRate(),
                sv.getSensors() != null ? new java.util.ArrayList<>(sv.getSensors()) : null,
                sv.getGeoAltitude(),
                sv.getSquawk() != null ? sv.getSquawk().toString() : null,
                sv.getSpi(),
                sv.getPositionSource(),
                sv.getCategory()
        );
        return Effect().persist(new AircraftStateEvent(attributes));
    }

    private Effect<AircraftStateEvent, AircraftState> onGetAircraftState(
            final AircraftState state,
            final AircraftStateCommand.GetAircraftState cmd) {
        final Optional<AircraftState> reply = Optional.ofNullable(
                state.attributes() != null && state.attributes().icao24() != null ? state : null);
        return Effect().reply(cmd.replyTo(), reply);
    }

    private AircraftState applyEvent(final AircraftState state, final AircraftStateEvent evt) {
        return new AircraftState(evt.attributes());
    }
}
