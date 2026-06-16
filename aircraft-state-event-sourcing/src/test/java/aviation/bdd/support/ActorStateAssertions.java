package aviation.bdd.support;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.javadsl.AskPattern;
import org.assertj.core.api.Assertions;

import aviation.actor.AircraftStatesManagerCommand;
import aviation.domain.AircraftAttributes;
import aviation.domain.AircraftState;

public final class ActorStateAssertions {

    private static final int ASK_TIMEOUT_SECONDS = 10;
    private static final int MAX_STATE_QUERY_ITERATIONS = 20;
    private static final int STATE_QUERY_WAIT_MILLIS = 500;

    private final ActorSystemSupport actorSystemSupport;

    public ActorStateAssertions(final ActorSystemSupport actorSystemSupport) {
        this.actorSystemSupport = actorSystemSupport;
    }

    public void assertActorExistsFor(final String icao24) throws Exception {
        final Optional<AircraftState> state = queryState(icao24);
        Assertions.assertThat(state).isPresent();
    }

    public void assertAircraftState(final String icao24,
                                    final String expectedCallsign,
                                    final String expectedOriginCountry,
                                    final double expectedLatitude,
                                    final double expectedLongitude) throws Exception {
        final Optional<AircraftState> state = queryState(icao24);
        Assertions.assertThat(state).isPresent();
        final AircraftAttributes attributes = state.get().attributes();
        Assertions.assertThat(attributes.callsign()).isEqualTo(expectedCallsign);
        Assertions.assertThat(attributes.originCountry()).isEqualTo(expectedOriginCountry);
        Assertions.assertThat(attributes.latitude()).isEqualTo(expectedLatitude);
        Assertions.assertThat(attributes.longitude()).isEqualTo(expectedLongitude);
    }

    private Optional<AircraftState> queryState(final String icao24) throws Exception {
        final Duration timeout = Duration.ofSeconds(ASK_TIMEOUT_SECONDS);
        for (int i = 0; i < MAX_STATE_QUERY_ITERATIONS; i++) {
            final CompletionStage<Optional<AircraftState>> future = AskPattern.ask(
                    actorSystemSupport.system(),
                    (ActorRef<Optional<AircraftState>> replyTo) -> new AircraftStatesManagerCommand.GetAircraftState(icao24, replyTo),
                    timeout,
                    actorSystemSupport.system().scheduler());
            final Optional<AircraftState> result = future.toCompletableFuture().get(ASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (result.isPresent()) {
                return result;
            }
            Thread.sleep(STATE_QUERY_WAIT_MILLIS);
        }
        return Optional.empty();
    }
}
