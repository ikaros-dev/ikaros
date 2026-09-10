package run.ikaros.search;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;

@Service
public class PersistentSearchReconciliationService implements SearchReconciliationService {
    private static final String PROJECTOR_VERSION = "resource-v1";
    private final SearchProjectionFailureRepository repository;
    private final run.ikaros.resource.api.ResourceSearchProjectionQuery source;
    private final SearchProjectionService projections;

    public PersistentSearchReconciliationService(SearchProjectionFailureRepository repository,
                                                 run.ikaros.resource.api.ResourceSearchProjectionQuery source,
                                                 SearchProjectionService projections) {
        this.repository = repository;
        this.source = source;
        this.projections = projections;
    }

    @Override
    public Flux<ProjectionFailureView> pendingFailures() {
        return repository.findByResolvedAtIsNullOrderByFailedAtAsc().take(100).map(this::view);
    }

    @Override
    public Mono<ProjectionFailureView> retry(UUID failureId) {
        return repository.findByIdAndResolvedAtIsNull(failureId)
            .switchIfEmpty(Mono.error(new NotFoundException("待处理的搜索投影失败不存在")))
            .flatMap(failure -> source.find(failure.sourceId())
                .switchIfEmpty(Mono.error(new NotFoundException("搜索投影来源资源不存在")))
                .flatMap(projection -> projections.project(projection.resourceId(), projection.sourceVersion(),
                    projection.fields(), PROJECTOR_VERSION, 0))
                .then(Mono.defer(() -> repository.save(new SearchProjectionFailureEntity(failure.id(),
                    failure.sourceId(), failure.sourceVersion(), failure.rebuildGeneration(), failure.reason(),
                    failure.failedAt(), Instant.now()))))
                .map(this::view));
    }

    private ProjectionFailureView view(SearchProjectionFailureEntity failure) {
        return new ProjectionFailureView(failure.id(), failure.sourceId(), failure.sourceVersion(),
            failure.rebuildGeneration(), failure.reason(), failure.failedAt(), failure.resolvedAt());
    }
}
