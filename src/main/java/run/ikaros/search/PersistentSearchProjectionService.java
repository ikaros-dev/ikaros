package run.ikaros.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** 基于 search_document 表的默认投影适配器。 */
@Primary
@Service
public class PersistentSearchProjectionService implements SearchProjectionService {
    private final SearchDocumentRepository repository;
    private final ObjectMapper mapper;
    private final AtomicLong generation = new AtomicLong();

    public PersistentSearchProjectionService(SearchDocumentRepository repository, ObjectMapper mapper) {
        this.repository = repository; this.mapper = mapper;
    }

    @Override
    public Mono<SearchDocument> project(UUID sourceId, long sourceVersion, Map<String, Object> fields,
                                        String projectorVersion, long rebuildGeneration) {
        if (sourceVersion < 0 || rebuildGeneration < 0) {
            return Mono.error(new IllegalArgumentException("投影版本不能为负数"));
        }
        return repository.findById(sourceId).flatMap(current -> current.sourceVersion() > sourceVersion
            ? fromEntity(current) : save(sourceId, sourceVersion, fields, projectorVersion, rebuildGeneration))
            .switchIfEmpty(save(sourceId, sourceVersion, fields, projectorVersion, rebuildGeneration));
    }

    @Override
    public Mono<Long> startRebuild() { return Mono.fromSupplier(generation::incrementAndGet); }

    @Override
    public Mono<SearchDocument> get(UUID sourceId) {
        return repository.findById(sourceId).flatMap(this::fromEntity);
    }

    @Override
    public Mono<ProjectionFailure> recordFailure(UUID sourceId, long sourceVersion,
                                                  long rebuildGeneration, String reason) {
        return Mono.just(new ProjectionFailure(sourceId, sourceVersion, rebuildGeneration,
            reason == null ? "unknown" : reason, Instant.now()));
    }

    private Mono<SearchDocument> save(UUID sourceId, long sourceVersion, Map<String, Object> fields,
                                      String projectorVersion, long rebuildGeneration) {
        try {
            Instant now = Instant.now();
            return repository.save(new SearchDocumentEntity(sourceId, sourceId, sourceVersion, projectorVersion,
                rebuildGeneration, mapper.writeValueAsString(fields == null ? Map.of() : fields), now))
                .map(entity -> new SearchDocument(entity.documentId(), entity.sourceId(), entity.sourceVersion(),
                    entity.projectorVersion(), entity.rebuildGeneration(), fields == null ? Map.of() : fields,
                    entity.projectedAt()));
        } catch (JsonProcessingException error) {
            return Mono.error(new IllegalArgumentException("搜索投影字段无法序列化", error));
        }
    }

    private Mono<SearchDocument> fromEntity(SearchDocumentEntity entity) {
        try {
            Map<String, Object> fields = mapper.readValue(entity.fieldsJson(), new TypeReference<>() { });
            return Mono.just(new SearchDocument(entity.documentId(), entity.sourceId(), entity.sourceVersion(),
                entity.projectorVersion(), entity.rebuildGeneration(), fields, entity.projectedAt()));
        } catch (JsonProcessingException error) {
            return Mono.error(new IllegalStateException("搜索投影数据损坏", error));
        }
    }
}
