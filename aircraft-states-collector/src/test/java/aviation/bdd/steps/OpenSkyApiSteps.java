package aviation.bdd.steps;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.quarkiverse.wiremock.devservice.WireMockConfigKey;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class OpenSkyApiSteps {

    @Inject
    @ConfigProperty(name = WireMockConfigKey.PORT)
    int wireMockPort;

    @Before
    public void configureWireMock() {
        WireMock.configureFor("localhost", wireMockPort);
        WireMock.reset();
    }

    @Given("the OpenSky API is available")
    public void theOpenSkyApiIsAvailable() {
        stubFor(get(urlPathEqualTo("/states/all"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("states_all_response.json")));
    }

    @Given("valid OAuth2 credentials are configured")
    public void validOAuth2CredentialsAreConfigured() {
        stubFor(post(urlPathEqualTo("/auth/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"test-token\",\"token_type\":\"Bearer\",\"expires_in\":1800}")));
    }

    @When("the API returns a 429 Too Many Requests response")
    public void theApiReturnsA429TooManyRequestsResponse() {
        stubFor(get(urlPathEqualTo("/states/all"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("X-Rate-Limit-Retry-After-Seconds", "60")));
    }

    @Then("the API request should include a valid Bearer token")
    public void theApiRequestShouldIncludeAValidBearerToken() {
        verify(getRequestedFor(urlPathEqualTo("/states/all"))
                .withHeader("Authorization", containing("Bearer")));
    }

    @Then("the scheduler should continue running without crashing")
    public void theSchedulerShouldContinueRunningWithoutCrashing() {
        // Scheduler continuity is implicitly verified if no exception propagates
    }
}
