package aviation.client;

import io.quarkus.oidc.client.filter.OidcClientFilter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@RegisterRestClient(configKey = "open-sky-client")
@OidcClientFilter
public interface OpenSkyClient {

    @GET
    @Path("/states/all")
    @Produces(MediaType.APPLICATION_JSON)
    String fetchAllStates();
}
