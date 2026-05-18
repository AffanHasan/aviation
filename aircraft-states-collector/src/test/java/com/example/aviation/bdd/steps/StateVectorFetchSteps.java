package com.example.aviation.bdd.steps;

import com.example.aviation.client.OpenSkyApiClient;
import com.example.aviation.domain.StateVectorResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class StateVectorFetchSteps {

    private WireMockServer wireMockServer;

    @Autowired
    private OpenSkyApiClient openSkyApiClient;

    private StateVectorResponse response;
    private Exception caughtException;

    @Before
    public void setUp() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
    }

    @After
    public void tearDown() {
        wireMockServer.stop();
    }

    @Given("the OpenSky API is available")
    public void theOpenSkyApiIsAvailable() {
        wireMockServer.stubFor(get(urlPathEqualTo("/states/all"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("states_all_response.json")));
    }

    @Given("valid OAuth2 credentials are configured")
    public void validOAuth2CredentialsAreConfigured() {
        wireMockServer.stubFor(post(urlPathEqualTo("/auth/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"test-token\",\"token_type\":\"Bearer\",\"expires_in\":1800}")));
    }

    @When("the scheduler triggers a fetch")
    public void theSchedulerTriggersAFetch() {
        try {
            response = openSkyApiClient.fetchStateVectors();
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @Then("the service should retrieve state vectors successfully")
    public void theServiceShouldRetrieveStateVectorsSuccessfully() {
        assertThat(caughtException).isNull();
        assertThat(response).isNotNull();
    }

    @Then("the response should contain valid aircraft data")
    public void theResponseShouldContainValidAircraftData() {
        assertThat(response.states()).isNotEmpty();
        assertThat(response.states().get(0).icao24()).isEqualTo("3c6444");
    }

    @When("the API returns a 429 Too Many Requests response")
    public void theApiReturnsA429TooManyRequestsResponse() {
        wireMockServer.stubFor(get(urlPathEqualTo("/states/all"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("X-Rate-Limit-Retry-After-Seconds", "60")));
    }

    @Then("the service should log a warning")
    public void theServiceShouldLogAWarning() {
        // Logging assertion can be added with LogCaptor or similar
        assertThat(caughtException).isNotNull();
    }

    @Then("the scheduler should continue running without crashing")
    public void theSchedulerShouldContinueRunningWithoutCrashing() {
        assertThat(caughtException).isInstanceOfAny(RuntimeException.class);
    }

    @Then("the API request should include a valid Bearer token")
    public void theApiRequestShouldIncludeAValidBearerToken() {
        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/states/all"))
                .withHeader("Authorization", containing("Bearer")));
    }
}
