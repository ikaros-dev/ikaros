package run.ikaros.search;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;

@Service
public class PersistentSearchReconciliationService implements SearchReconciliationService {
    private final SearchProjectionFailureRepository repository;

    public PersistentSearchReconciliationService(SearchProjectionFailureRepository repository) {
        this.repository = repository;
    }

    @Override
    public Flux<ProjectionFailureView> pendingFailures() {
        return repository.findByResolvedAtIsNullOrderByFailedAtAsc().map(this::view);
    }

    @Override
    public Mono<Void> resolve(UUID failureId) {
        return repository.findByIdAndResolvedAtIsNull(failureId)
            .switchIfEmpty(Mono.error(new NotFoundException("待处理的搜索投影失败不存在")))
            .flatMap(failure -> repository.save(new SearchProjectionFailureEntity(failure.id(), failure.sourceId(),
                failure.sourceVersion(), failure.rebuildGeneration(), failure.reason(), failure.failedAt(),
                Instant.now())))
            .then();
    }

    private ProjectionFailureView view(SearchProjectionFailureEntity failure) {
        return new ProjectionFailureView(failure.id(), failure.sourceId(), failure.sourceVersion(),
            failure.rebuildGeneration(), failure.reason(), failure.failedAt(), failure.resolvedAt());
    }
}
