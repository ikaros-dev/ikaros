package run.ikaros.ingestion;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.audit.AuditService;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.task.BackgroundTaskService;
import run.ikaros.event.DurableEventService;
@Service
public class DefaultImportRunService implements ImportRunService {
    private final ImportPlanRepository plans; private final ImportRunRepository runs;
    private final BackgroundTaskService tasks; private final AuditService audit; private final DurableEventService events;
    public DefaultImportRunService(ImportPlanRepository plans, ImportRunRepository runs, BackgroundTaskService tasks, AuditService audit) {
        this(plans,runs,tasks,audit,null);
    }
    @org.springframework.beans.factory.annotation.Autowired
    public DefaultImportRunService(ImportPlanRepository plans, ImportRunRepository runs, BackgroundTaskService tasks,
        AuditService audit, DurableEventService events) {
        this.plans=plans; this.runs=runs; this.tasks=tasks; this.audit=audit; this.events=events;
    }
    public Mono<ImportRunView> start(UUID ownerId, UUID planId, StartImportRequest request) {
        return plans.findByIdAndOwnerId(planId, ownerId).switchIfEmpty(Mono.error(new NotFoundException("Import Plan 不存在或无权访问")))
            .flatMap(plan -> { if (plan.version()!=null && request.expectedPlanVersion()!=plan.version())
                return Mono.<ImportPlanEntity>error(new ConflictException("Import Plan 版本已过期")); return Mono.just(plan); })
            .flatMap(plan -> tasks.submit("ingestion.import", Map.of("plan_id", planId.toString(), "actor_id", ownerId.toString()),
                "ingestion.import:"+planId+":"+request.expectedPlanVersion()))
            .flatMap(task -> { Instant now=Instant.now(); return runs.save(new ImportRunEntity(null, planId, ownerId, ownerId,
                ImportRunStatus.PENDING.name(), null, 0, 0, 0, task.id(), now, null, now, null)); })
            .flatMap(run -> (events == null ? Mono.empty() : events.append("ingestion.import.started", 1,
                "ingestion_import_run", run.id(), "{\"run_id\":\"" + run.id() + "\",\"plan_id\":\""
                    + run.planId() + "\"}").then()).then(audit.record(ownerId,"ingestion.import.start",
                        "INGESTION_IMPORT_RUN",run.id(),"{}")).thenReturn(view(run)));
    }
    public Mono<List<ImportRunView>> list(UUID ownerId) { return runs.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).map(this::view).collectList(); }
    public Mono<ImportRunView> get(UUID ownerId, UUID runId) { return owned(ownerId,runId).map(this::view); }
    public Mono<ImportRunView> cancel(UUID ownerId, UUID runId) { return owned(ownerId,runId).flatMap(run -> {
        if (ImportRunStatus.SUCCEEDED.name().equals(run.status()) || ImportRunStatus.CANCELLED.name().equals(run.status()))
            return Mono.error(new ConflictException("导入运行当前状态不允许取消"));
        return runs.save(copy(run,ImportRunStatus.CANCELLED.name(),run.checkpoint(),run.completedCount(),run.failedCount(),run.skippedCount(),Instant.now())).map(this::view);
    }); }
    public Mono<ImportRunView> checkpoint(UUID id,String checkpoint,long completed,long failed,long skipped) {
        if(completed<0||failed<0||skipped<0) return Mono.error(new IllegalArgumentException("导入统计不能为负数"));
        return runs.findById(id).switchIfEmpty(Mono.error(new NotFoundException("导入运行不存在")))
            .flatMap(run->runs.save(copy(run,ImportRunStatus.RUNNING.name(),checkpoint,completed,failed,skipped,run.finishedAt())).map(this::view));
    }
    private Mono<ImportRunEntity> owned(UUID owner,UUID id){return runs.findByIdAndOwnerId(id,owner).switchIfEmpty(Mono.error(new NotFoundException("导入运行不存在或无权访问")));}
    private ImportRunEntity copy(ImportRunEntity r,String s,String c,long a,long f,long k,Instant end){return new ImportRunEntity(r.id(),r.planId(),r.ownerId(),r.actorId(),s,c,a,f,k,r.backgroundTaskId(),r.startedAt(),end,r.createdAt(),r.version());}
    private ImportRunView view(ImportRunEntity r){return new ImportRunView(r.id(),r.planId(),r.actorId(),ImportRunStatus.valueOf(r.status()),r.checkpoint(),r.completedCount(),r.failedCount(),r.skippedCount(),r.backgroundTaskId(),r.startedAt(),r.finishedAt(),r.createdAt());}
}
