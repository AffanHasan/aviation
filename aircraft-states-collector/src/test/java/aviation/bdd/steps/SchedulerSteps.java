package aviation.bdd.steps;

import aviation.domain.StateVectorResponse;
import aviation.service.StateVectorFetcherService;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;

public class SchedulerSteps {

    @Inject
    StateVectorFetcherService fetcherService;

    private StateVectorResponse response;
    private Exception caughtException;

    @When("the scheduler triggers a fetch")
    public void theSchedulerTriggersAFetch() {
        try {
            response = fetcherService.fetchStateVectors();
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @When("the scheduler triggers a fetch via the service")
    public void theSchedulerTriggersAFetchViaTheService() {
        try {
            fetcherService.fetchAndProcess();
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @Then("the service should retrieve state vectors successfully")
    public void theServiceShouldRetrieveStateVectorsSuccessfully() {
        assertNull(caughtException, "Expected no exception but got: " + caughtException);
        assertNotNull(response);
    }

    @Then("the response should contain valid aircraft data")
    public void theResponseShouldContainValidAircraftData() {
        assertNotNull(response.states());
        assertFalse(response.states().isEmpty());
        assertEquals("3c6444", response.states().get(0).icao24());
    }

    @Then("the service should log a warning")
    public void theServiceShouldLogAWarning() {
        assertNotNull(caughtException);
    }
}
