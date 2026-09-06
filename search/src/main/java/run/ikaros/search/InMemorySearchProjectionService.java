package run.ikaros.search;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** P1 默认投影实现；可替换为 Lucene/OpenSearch adapter。 */
@Service
public class InMemorySearchProjectionService implements SearchProjectionService {
    private final Map<UUID, SearchDocument> documents = new ConcurrentHashMap<>();
    private final AtomicLong generation = new AtomicLong();

    @Override
    public Mono<SearchDocument> project(UUID sourceId, long sourceVersion, Map<String, Object> fields,
                                        String projectorVersion, long rebuildGeneration) {
        if (sourceVersion < 0 || rebuildGeneration < 0) {
            return Mono.error(new IllegalArgumentException("投影版本不能为负数"));
        }
        return Mono.fromSupplier(() -> {
            SearchDocument candidate = new SearchDocument(sourceId, sourceId, sourceVersion,
                projectorVersion, rebuildGeneration, fields, Instant.now());
            documents.compute(sourceId, (ignored, current) -> current == null
                || !isOlder(current, candidate) ? candidate : current);
            return documents.get(sourceId);
        });
    }

    @Override
    public Mono<Long> startRebuild() {
        return Mono.fromSupplier(generation::incrementAndGet);
    }

    @Override
    public Mono<SearchDocument> get(UUID sourceId) {
        return Mono.justOrEmpty(documents.get(sourceId));
    }

    @Override
    public Mono<ProjectionFailure> recordFailure(UUID sourceId, long sourceVersion,
                                                  long rebuildGeneration, String reason) {
        return Mono.just(new ProjectionFailure(sourceId, sourceVersion, rebuildGeneration,
            reason == null ? "unknown" : reason, Instant.now()));
    }

    private boolean isOlder(SearchDocument current, SearchDocument candidate) {
        if (current.sourceVersion() != candidate.sourceVersion()) {
            return current.sourceVersion() > candidate.sourceVersion();
        }
        if (current.rebuildGeneration() != candidate.rebuildGeneration()) {
            return current.rebuildGeneration() > candidate.rebuildGeneration();
        }
        return java.util.Objects.equals(current.projectorVersion(), candidate.projectorVersion());
    }
}
