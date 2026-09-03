package run.ikaros.search;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SearchReconciliationService {
    Flux<ProjectionFailureView> pendingFailures();
    Mono<Void> resolve(UUID failureId);
}
