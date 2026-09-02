package run.ikaros.ingestion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.audit.AuditService;
import run.ikaros.common.NotFoundException;

@Service
public class DefaultIngestionSourceService implements IngestionSourceService {
    private final IngestionSourceRepository repository;
    private final AuditService auditService;
    private final ObjectMapper mapper;

    public DefaultIngestionSourceService(IngestionSourceRepository repository, AuditService auditService,
                                         ObjectMapper mapper) {
        this.repository = repository;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    @Override
    public Mono<IngestionSourceView> create(UUID ownerId, CreateIngestionSourceRequest request) {
        if (request.credentialReference() != null && !request.credentialReference().startsWith("secret://")) {
            return Mono.error(new IllegalArgumentException("credential reference 必须使用 secret:// URI"));
        }
        Instant now = Instant.now();
        return encode(request.scanPolicy()).flatMap(policy -> repository.save(new IngestionSourceEntity(
            null, ownerId, request.type().name(), request.displayName(), request.rootReference(),
            request.credentialReference(), policy, IngestionSourceStatus.ENABLED.name(), null,
            "UNKNOWN", now, now, null)))
            .flatMap(source -> auditService.record(ownerId, "ingestion.source.create", "INGESTION_SOURCE",
                source.id(), "{}").thenReturn(toView(source)));
    }

    @Override
    public Mono<List<IngestionSourceView>> list(UUID ownerId) {
        return repository.findAllByOwnerIdOrderByUpdatedAtDesc(ownerId).map(this::toView).collectList();
    }

    @Override
    public Mono<IngestionSourceView> get(UUID ownerId, UUID sourceId) {
        return owned(ownerId, sourceId).map(this::toView);
    }

    @Override
    public Mono<IngestionSourceView> enable(UUID ownerId, UUID sourceId) {
        return change(ownerId, sourceId, IngestionSourceStatus.ENABLED, "ingestion.source.enable");
    }

    @Override
    public Mono<IngestionSourceView> disable(UUID ownerId, UUID sourceId) {
        return change(ownerId, sourceId, IngestionSourceStatus.DISABLED, "ingestion.source.disable");
    }

    private Mono<IngestionSourceView> change(UUID ownerId, UUID sourceId, IngestionSourceStatus status, String action) {
        return owned(ownerId, sourceId).flatMap(current -> repository.save(new IngestionSourceEntity(
            current.id(), current.ownerId(), current.sourceType(), current.displayName(), current.rootReference(),
            current.credentialReference(), current.scanPolicyJson(), status.name(), current.lastSuccessfulScan(),
            current.healthStatus(), current.createdAt(), Instant.now(), current.version())))
            .flatMap(saved -> auditService.record(ownerId, action, "INGESTION_SOURCE", sourceId, "{}").thenReturn(toView(saved)));
    }

    private Mono<IngestionSourceEntity> owned(UUID ownerId, UUID sourceId) {
        return repository.findByIdAndOwnerId(sourceId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("Ingestion Source 不存在或无权访问")));
    }

    private Mono<String> encode(Map<String, Object> policy) {
        try {
            return Mono.just(mapper.writeValueAsString(policy == null ? Map.of() : policy));
        } catch (JsonProcessingException error) {
            return Mono.error(new IllegalArgumentException("scan policy 无法序列化", error));
        }
    }

    private IngestionSourceView toView(IngestionSourceEntity source) {
        try {
            Map<String, Object> policy = mapper.readValue(source.scanPolicyJson(), new TypeReference<>() { });
            return new IngestionSourceView(source.id(), IngestionSourceType.valueOf(source.sourceType()),
                source.displayName(), source.rootReference(), source.credentialReference() != null, policy,
                IngestionSourceStatus.valueOf(source.status()), source.lastSuccessfulScan(), source.healthStatus(),
                source.createdAt(), source.updatedAt());
        } catch (JsonProcessingException | IllegalArgumentException error) {
            throw new IllegalStateException("Ingestion Source 数据损坏", error);
        }
    }
}
