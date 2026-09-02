package run.ikaros.ingestion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.event.DurableEventService;
interface ImportPlanService {
    Mono<ImportPlanView> generate(UUID ownerId, UUID scanRunId, GenerateImportPlanRequest request);
    Mono<List<ImportPlanItemEntity>> items(UUID ownerId, UUID planId);
    Mono<ImportPlanView> approve(UUID ownerId, UUID planId, ApproveImportPlanRequest request);
    Mono<ImportPlanItemEntity> updateItem(UUID ownerId, UUID planId, UUID itemId, UpdateImportPlanItemRequest request);
}

@Service
class DefaultImportPlanService implements ImportPlanService {
    private final ScanRunRepository scans; private final IngestionCandidateRepository candidates;
    private final ImportPlanRepository plans; private final ImportPlanItemRepository items; private final ObjectMapper mapper;
    private final DurableEventService events;
    DefaultImportPlanService(ScanRunRepository scans, IngestionCandidateRepository candidates, ImportPlanRepository plans,
        ImportPlanItemRepository items, ObjectMapper mapper) { this(scans,candidates,plans,items,mapper,null); }
    @org.springframework.beans.factory.annotation.Autowired
    DefaultImportPlanService(ScanRunRepository scans, IngestionCandidateRepository candidates, ImportPlanRepository plans,
        ImportPlanItemRepository items, ObjectMapper mapper, DurableEventService events) { this.scans=scans; this.candidates=candidates; this.plans=plans; this.items=items; this.mapper=mapper; this.events=events; }
    public Mono<ImportPlanView> generate(UUID ownerId, UUID scanId, GenerateImportPlanRequest request) {
        return scans.findByIdAndOwnerId(scanId, ownerId).switchIfEmpty(Mono.error(new NotFoundException("扫描运行不存在或无权访问")))
            .then(encode(request.policySnapshot())).flatMap(policy -> plans.save(new ImportPlanEntity(null, scanId, ownerId,
                request.dryRun(), "GENERATED", policy, Instant.now(), null)))
            .flatMap(plan -> candidates.findAllByScanRunIdOrderByCreatedAtAsc(scanId)
                .flatMap(candidate -> items.save(new ImportPlanItemEntity(null, plan.id(), candidate.id(),
                    ImportAction.REQUIRE_REVIEW.name(), null, "等待匹配策略确认", candidate.confidence(),
                    plan.id()+":"+candidate.id(), Instant.now(), null))).count().map(count -> new ImportPlanView(
                        plan.id(), plan.scanRunId(), plan.dryRun(), plan.status(), plan.version(), plan.generatedAt(), count))
                    .flatMap(view -> events == null ? Mono.just(view) : events.append("ingestion.plan.generated", 1,
                        "ingestion_import_plan", plan.id(), "{\"plan_id\":\"" + plan.id()
                            + "\",\"scan_run_id\":\"" + plan.scanRunId() + "\"}").thenReturn(view)));
    }
    public Mono<List<ImportPlanItemEntity>> items(UUID ownerId, UUID planId) {
        return plans.findByIdAndOwnerId(planId, ownerId).switchIfEmpty(Mono.error(new NotFoundException("Import Plan 不存在或无权访问")))
            .thenMany(items.findAllByPlanIdOrderByCreatedAtAsc(planId)).collectList();
    }
    public Mono<ImportPlanView> approve(UUID ownerId, UUID planId, ApproveImportPlanRequest request) {
        return plans.findByIdAndOwnerId(planId, ownerId).switchIfEmpty(Mono.error(new NotFoundException("Import Plan 不存在或无权访问")))
            .flatMap(plan -> { if (!"GENERATED".equals(plan.status())) return Mono.error(new IllegalStateException("计划当前不可审批"));
                if (plan.version()!=null && !plan.version().equals(request.expectedVersion())) return Mono.error(new run.ikaros.common.ConflictException("Import Plan 版本已过期"));
                return plans.save(new ImportPlanEntity(plan.id(),plan.scanRunId(),plan.ownerId(),plan.dryRun(),"APPROVED",plan.policySnapshotJson(),plan.generatedAt(),plan.version())); })
            .flatMap(plan -> items.findAllByPlanIdOrderByCreatedAtAsc(plan.id()).collectList().flatMap(all -> {
                if (all.stream().anyMatch(item -> "REQUIRE_REVIEW".equals(item.action()) || "CONFLICT".equals(item.action())))
                    return Mono.error(new run.ikaros.common.ConflictException("Import Plan 仍有未解决的审核项"));
                return Mono.just(new ImportPlanView(plan.id(),plan.scanRunId(),plan.dryRun(),plan.status(),plan.version(),plan.generatedAt(),all.size()));
            }));
    }
    public Mono<ImportPlanItemEntity> updateItem(UUID ownerId, UUID planId, UUID itemId, UpdateImportPlanItemRequest request) {
        return plans.findByIdAndOwnerId(planId, ownerId).switchIfEmpty(Mono.error(new NotFoundException("Import Plan 不存在或无权访问")))
            .flatMap(plan -> { if (!"GENERATED".equals(plan.status())) return Mono.error(new run.ikaros.common.ConflictException("已审批计划不可修改"));
                return items.findById(itemId).switchIfEmpty(Mono.error(new NotFoundException("计划项不存在")))
                    .flatMap(item -> { if (!planId.equals(item.planId())) return Mono.error(new NotFoundException("计划项不存在"));
                        if (item.version()!=null && !item.version().equals(request.expectedVersion())) return Mono.error(new run.ikaros.common.ConflictException("计划项版本已过期"));
                        return items.save(new ImportPlanItemEntity(item.id(),item.planId(),item.candidateId(),request.action().name(),request.targetId(),request.reason(),item.confidence(),item.idempotencyKey(),item.createdAt(),item.version())); }); });
    }
    private Mono<String> encode(Map<String,Object> value) { try { return Mono.just(mapper.writeValueAsString(value==null?Map.of():value)); }
        catch (JsonProcessingException e) { return Mono.error(new IllegalArgumentException("policy snapshot 无法序列化", e)); } }
}
