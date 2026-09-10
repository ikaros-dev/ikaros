package run.ikaros.ingestion;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.integration.api.EventAppendRequest;
import run.ikaros.operations.api.AuditService;

@Service
public class DefaultMetadataSyncSourceService implements MetadataSyncSourceService {
    private static final int MAX_RESULTS = 100;
    private static final Set<String> SCHEDULES = Set.of("MANUAL", "HOURLY", "DAILY");
    private final MetadataSyncSourceRepository repository;
    private final AuditService auditService;
    private final DurableEventPublisher events;

    public DefaultMetadataSyncSourceService(MetadataSyncSourceRepository repository, AuditService auditService,
                                            DurableEventPublisher events) {
        this.repository = repository;
        this.auditService = auditService;
        this.events = events;
    }

    @Override
    public Mono<MetadataSyncSourceView> create(UUID ownerId, CreateMetadataSyncSourceRequest request) {
        if (request.credentialReference() != null && !request.credentialReference().isBlank()
            && !request.credentialReference().startsWith("secret://")) {
            return Mono.error(new IllegalArgumentException("credential reference 必须使用 secret:// URI"));
        }
        String schedule = normalizeSchedule(request.refreshSchedule());
        Instant now = Instant.now();
        return repository.save(new MetadataSyncSourceEntity(null, ownerId, request.providerKey().trim(),
                request.displayName().trim(), blankToNull(request.credentialReference()), schedule,
                MetadataSyncSourceStatus.ENABLED.name(), now, now, null))
            .flatMap(saved -> emit("metadata.sync-source.created", saved.id(), ownerId)
                .then(auditService.record(ownerId, "metadata.sync-source.create", "METADATA_SYNC_SOURCE",
                    saved.id(), "{}"))
                .thenReturn(view(saved)));
    }

    @Override
    public Mono<List<MetadataSyncSourceView>> list(UUID ownerId) {
        return repository.findAllByOwnerIdOrderByUpdatedAtDesc(ownerId).take(MAX_RESULTS)
            .map(this::view).collectList();
    }

    @Override
    public Mono<MetadataSyncSourceView> enable(UUID ownerId, UUID sourceId) {
        return change(ownerId, sourceId, MetadataSyncSourceStatus.ENABLED, "metadata.sync-source.enable");
    }

    @Override
    public Mono<MetadataSyncSourceView> disable(UUID ownerId, UUID sourceId) {
        return change(ownerId, sourceId, MetadataSyncSourceStatus.DISABLED, "metadata.sync-source.disable");
    }

    private Mono<MetadataSyncSourceView> change(UUID ownerId, UUID sourceId, MetadataSyncSourceStatus status,
                                                String action) {
        return owned(ownerId, sourceId).flatMap(current -> repository.save(new MetadataSyncSourceEntity(
                current.id(), current.ownerId(), current.providerKey(), current.displayName(),
                current.credentialReference(), current.refreshSchedule(), status.name(), current.createdAt(),
                Instant.now(), current.version())))
            .flatMap(saved -> emit("metadata.sync-source." + status.name().toLowerCase(Locale.ROOT), sourceId, ownerId)
                .then(auditService.record(ownerId, action, "METADATA_SYNC_SOURCE", sourceId, "{}"))
                .thenReturn(view(saved)));
    }

    private Mono<MetadataSyncSourceEntity> owned(UUID ownerId, UUID sourceId) {
        return repository.findByIdAndOwnerId(sourceId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("元数据同步来源不存在或无权访问")));
    }

    private String normalizeSchedule(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!SCHEDULES.contains(normalized)) {
            throw new IllegalArgumentException("refresh_schedule 必须是 MANUAL、HOURLY 或 DAILY");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Mono<Void> emit(String type, UUID sourceId, UUID ownerId) {
        if (events == null) return Mono.empty();
        return events.append(new EventAppendRequest(type, 1, "metadata", "metadata_sync_source", sourceId,
            "{\"source_id\":\"" + sourceId + "\",\"owner_id\":\"" + ownerId + "\"}"))
            .then();
    }

    private MetadataSyncSourceView view(MetadataSyncSourceEntity source) {
        return new MetadataSyncSourceView(source.id(), source.providerKey(), source.displayName(),
            source.credentialReference() != null, source.refreshSchedule(),
            MetadataSyncSourceStatus.valueOf(source.status()), source.createdAt(), source.updatedAt(), source.version());
    }
}
