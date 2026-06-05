package aviation.service;

import aviation.client.OpenSkyClient;
import aviation.client.StateVectorConverter;
import aviation.domain.StateVectorResponse;
import aviation.kafka.StateVectorKafkaPublisher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class StateVectorFetcherService {

    @Inject
    @RestClient
    OpenSkyClient openSkyClient;

    @Inject
    StateVectorKafkaPublisher kafkaPublisher;

    public void fetchAndProcess() {
        try {
            StateVectorResponse response = fetchStateVectors();
            kafkaPublisher.publish(response);
        } catch (Exception e) {
            // Log and continue - scheduler must not crash
            System.err.println("Failed to fetch or publish state vectors: " + e.getMessage());
        }
    }

    public StateVectorResponse fetchStateVectors() {
        String json = openSkyClient.fetchAllStates();
        return StateVectorConverter.fromJson(json);
    }
}
