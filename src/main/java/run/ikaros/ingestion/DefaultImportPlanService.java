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
interface ImportPlanService {
    Mono<ImportPlanView> generate(UUID ownerId, UUID scanRunId, GenerateImportPlanRequest request);
    Mono<List<ImportPlanItemEntity>> items(UUID ownerId, UUID planId);
}

@Service
class DefaultImportPlanService implements ImportPlanService {
    private final ScanRunRepository scans; private final IngestionCandidateRepository candidates;
    private final ImportPlanRepository plans; private final ImportPlanItemRepository items; private final ObjectMapper mapper;
    DefaultImportPlanService(ScanRunRepository scans, IngestionCandidateRepository candidates, ImportPlanRepository plans,
        ImportPlanItemRepository items, ObjectMapper mapper) { this.scans=scans; this.candidates=candidates; this.plans=plans; this.items=items; this.mapper=mapper; }
    public Mono<ImportPlanView> generate(UUID ownerId, UUID scanId, GenerateImportPlanRequest request) {
        return scans.findByIdAndOwnerId(scanId, ownerId).switchIfEmpty(Mono.error(new NotFoundException("扫描运行不存在或无权访问")))
            .then(encode(request.policySnapshot())).flatMap(policy -> plans.save(new ImportPlanEntity(null, scanId, ownerId,
                request.dryRun(), "GENERATED", policy, Instant.now(), null)))
            .flatMap(plan -> candidates.findAllByScanRunIdOrderByCreatedAtAsc(scanId)
                .flatMap(candidate -> items.save(new ImportPlanItemEntity(null, plan.id(), candidate.id(),
                    ImportAction.REQUIRE_REVIEW.name(), null, "等待匹配策略确认", candidate.confidence(),
                    plan.id()+":"+candidate.id(), Instant.now(), null))).count().map(count -> new ImportPlanView(
                        plan.id(), plan.scanRunId(), plan.dryRun(), plan.status(), plan.generatedAt(), count)));
    }
    public Mono<List<ImportPlanItemEntity>> items(UUID ownerId, UUID planId) {
        return plans.findByIdAndOwnerId(planId, ownerId).switchIfEmpty(Mono.error(new NotFoundException("Import Plan 不存在或无权访问")))
            .thenMany(items.findAllByPlanIdOrderByCreatedAtAsc(planId)).collectList();
    }
    private Mono<String> encode(Map<String,Object> value) { try { return Mono.just(mapper.writeValueAsString(value==null?Map.of():value)); }
        catch (JsonProcessingException e) { return Mono.error(new IllegalArgumentException("policy snapshot 无法序列化", e)); } }
}
