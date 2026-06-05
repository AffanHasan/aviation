package aviation.scheduler;

import aviation.service.StateVectorFetcherService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StateVectorScheduler {

    @Inject
    StateVectorFetcherService fetcherService;

    @Scheduled(every = "${state-collector.interval}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void scheduledFetch() {
        fetcherService.fetchAndProcess();
    }
}
