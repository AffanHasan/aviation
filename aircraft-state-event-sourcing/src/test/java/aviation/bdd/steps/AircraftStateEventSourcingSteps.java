package aviation.bdd.steps;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;

import com.example.aviation.avro.StateVectorAvro;
import com.example.aviation.avro.StateVectorResponseAvro;

import aviation.bdd.support.ActorStateAssertions;
import aviation.bdd.support.ActorSystemSupport;
import aviation.bdd.support.EventJournalAssertions;
import aviation.bdd.support.KafkaMessageProducer;
import aviation.bdd.support.TestcontainersSupport;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class AircraftStateEventSourcingSteps {

    private static final String KAFKA_TOPIC = "aircraft.state.vectors";

    private static final TestcontainersSupport INFRASTRUCTURE = new TestcontainersSupport();
    private static final ActorSystemSupport ACTOR_SYSTEM = new ActorSystemSupport();

    private final KafkaMessageProducer producer;
    private final EventJournalAssertions journalAssertions;
    private final ActorStateAssertions stateAssertions;

    private final List<String> publishedIcao24s = new ArrayList<>();

    public AircraftStateEventSourcingSteps() {
        this.producer = new KafkaMessageProducer(INFRASTRUCTURE.kafkaBootstrapServers());
        this.journalAssertions = new EventJournalAssertions(INFRASTRUCTURE);
        this.stateAssertions = new ActorStateAssertions(ACTOR_SYSTEM);
    }

    @BeforeAll
    public static void beforeAll() throws Exception {
        INFRASTRUCTURE.start();
        ACTOR_SYSTEM.start(INFRASTRUCTURE);
    }

    @AfterAll
    public static void afterAll() throws Exception {
        ACTOR_SYSTEM.terminate();
        INFRASTRUCTURE.stop();
    }

    @After
    public void afterEach() throws Exception {
        journalAssertions.truncate();
        publishedIcao24s.clear();
    }

    @Given("Kafka and PostgreSQL are running")
    public void kafkaAndPostgreSQLAreRunning() {
        Assertions.assertThat(INFRASTRUCTURE.isRunning()).isTrue();
    }

    @Given("the event-sourcing actor system is started")
    public void theEventSourcingActorSystemIsStarted() {
        Assertions.assertThat(ACTOR_SYSTEM.isRunning()).isTrue();
    }

    @When("3 sample aircraft state messages are consumed from the {string} topic")
    public void sampleAircraftStateMessagesAreConsumedFromTheTopic(final String topic) throws Exception {
        final List<StateVectorAvro> states = List.of(
                producer.buildStateVector("3c6444", "DLH123", "Germany", 1715974495, 50.1109, 8.6821),
                producer.buildStateVector("a0b1c2", "AFR456", "France", 1715974496, 48.8566, 2.3522),
                producer.buildStateVector("d4e5f6", "BAW789", "United Kingdom", 1715974497, 51.4700, -0.4543)
        );

        states.stream()
                .map(sv -> sv.getIcao24().toString())
                .forEach(publishedIcao24s::add);

        final StateVectorResponseAvro response = new StateVectorResponseAvro();
        response.setTime(1715974500L);
        response.setStates(new ArrayList<>(states));

        producer.produceStateVectorResponse(topic, response);
        journalAssertions.waitForEvents(publishedIcao24s.size());
    }

    @Then("3 aircraft state events are persisted in the database")
    public void aircraftStateEventsArePersistedInTheDatabase() throws Exception {
        journalAssertions.assertEventsPersistedFor(publishedIcao24s);
    }

    @Then("3 event sourced persistent actors exist in memory")
    public void eventSourcedPersistentActorsExistInMemory() throws Exception {
        for (final String icao24 : publishedIcao24s) {
            stateAssertions.assertActorExistsFor(icao24);
        }
    }

    @Then("each actor represents the latest aircraft state")
    public void eachActorRepresentsTheLatestAircraftState() throws Exception {
        stateAssertions.assertAircraftState("3c6444", "DLH123", "Germany", 50.1109, 8.6821);
        stateAssertions.assertAircraftState("a0b1c2", "AFR456", "France", 48.8566, 2.3522);
        stateAssertions.assertAircraftState("d4e5f6", "BAW789", "United Kingdom", 51.4700, -0.4543);
    }
}
