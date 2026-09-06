package run.ikaros.ingestion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.audit.AuditService;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.task.BackgroundTask;
import run.ikaros.task.BackgroundTaskService;

@Service
public class DefaultScanRunService implements ScanRunService {
    private static final int MAX_UNPAGED_RESULTS = 100;
    private final IngestionSourceRepository sourceRepository;
    private final ScanRunRepository repository;
    private final BackgroundTaskService taskService;
    private final AuditService auditService;

    public DefaultScanRunService(IngestionSourceRepository sourceRepository, ScanRunRepository repository,
                                 BackgroundTaskService taskService, AuditService auditService) {
        this.sourceRepository = sourceRepository;
        this.repository = repository;
        this.taskService = taskService;
        this.auditService = auditService;
    }

    @Override
    public Mono<ScanRunView> start(UUID ownerId, UUID sourceId, StartScanRequest request) {
        return sourceRepository.findByIdAndOwnerId(sourceId, ownerId)
            .filter(source -> IngestionSourceStatus.ENABLED.name().equals(source.status()))
            .switchIfEmpty(Mono.error(new ConflictException("Source 不存在或当前未启用")))
            .flatMap(source -> taskService.submit("ingestion.scan", java.util.Map.of(
                "source_id", sourceId.toString(), "trigger", request.trigger(), "actor_id", ownerId.toString()),
                "ingestion.scan:" + sourceId))
            .flatMap(task -> saveRun(ownerId, sourceId, request.trigger(), task));
    }

    private Mono<ScanRunView> saveRun(UUID ownerId, UUID sourceId, String trigger, BackgroundTask task) {
        Instant now = Instant.now();
        return repository.save(new ScanRunEntity(null, sourceId, ownerId, trigger, ownerId,
            ScanRunStatus.PENDING.name(), null, 0, 0, 0, null, task.id(), null, null, now, null))
            .flatMap(run -> auditService.record(ownerId, "ingestion.scan.start", "INGESTION_SCAN", run.id(), "{}")
                .thenReturn(toView(run)));
    }

    @Override public Mono<List<ScanRunView>> list(UUID ownerId) {
        return repository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).take(MAX_UNPAGED_RESULTS)
            .map(this::toView).collectList();
    }

    @Override public Mono<ScanRunView> get(UUID ownerId, UUID scanId) {
        return owned(ownerId, scanId).map(this::toView);
    }

    @Override public Mono<ScanRunView> cancel(UUID ownerId, UUID scanId) {
        return owned(ownerId, scanId).flatMap(run -> {
            if (ScanRunStatus.SUCCEEDED.name().equals(run.status()) || ScanRunStatus.CANCELLED.name().equals(run.status())) {
                return Mono.error(new ConflictException("扫描当前状态不允许取消"));
            }
            return repository.save(copy(run, ScanRunStatus.CANCELLED.name(), run.checkpoint(), run.discoveredCount(),
                run.changedCount(), run.skippedCount(), run.errorSummary(), Instant.now()))
                .flatMap(saved -> auditService.record(ownerId, "ingestion.scan.cancel", "INGESTION_SCAN", scanId, "{}").thenReturn(toView(saved)));
        });
    }

    @Override public Mono<ScanRunView> checkpoint(UUID scanId, String checkpoint, long discovered, long changed,
                                                   long skipped, String errorSummary) {
        if (discovered < 0 || changed < 0 || skipped < 0) return Mono.error(new IllegalArgumentException("扫描统计不能为负数"));
        return repository.findById(scanId).switchIfEmpty(Mono.error(new NotFoundException("扫描运行不存在")))
            .flatMap(run -> repository.save(copy(run, ScanRunStatus.RUNNING.name(), checkpoint, discovered, changed,
                skipped, errorSummary, run.finishedAt())).map(this::toView));
    }

    private Mono<ScanRunEntity> owned(UUID ownerId, UUID scanId) {
        return repository.findByIdAndOwnerId(scanId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("扫描运行不存在或无权访问")));
    }

    private ScanRunEntity copy(ScanRunEntity run, String status, String checkpoint, long discovered, long changed,
                               long skipped, String error, Instant finishedAt) {
        return new ScanRunEntity(run.id(), run.sourceId(), run.ownerId(), run.trigger(), run.actorId(), status,
            checkpoint, discovered, changed, skipped, error, run.backgroundTaskId(), run.startedAt(), finishedAt,
            run.createdAt(), run.version());
    }

    private ScanRunView toView(ScanRunEntity run) {
        return new ScanRunView(run.id(), run.sourceId(), run.trigger(), run.actorId(), ScanRunStatus.valueOf(run.status()),
            run.checkpoint(), run.discoveredCount(), run.changedCount(), run.skippedCount(), run.errorSummary(),
            run.backgroundTaskId(), run.startedAt(), run.finishedAt(), run.createdAt());
    }
}
