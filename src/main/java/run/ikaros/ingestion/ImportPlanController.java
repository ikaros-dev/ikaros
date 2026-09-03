package run.ikaros.ingestion;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
@RestController
@RequestMapping({"/api/ingestion/scans"})
public class ImportPlanController {
    private final ImportPlanService service;
    public ImportPlanController(ImportPlanService service) { this.service=service; }
    @PostMapping("/{scanId}/plans")
    public Mono<ImportPlanView> generate(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId, @PathVariable UUID scanId,
        @Valid @RequestBody GenerateImportPlanRequest request) { return service.generate(actorId, scanId, request); }
    @GetMapping("/plans/{planId}/items")
    public Mono<List<ImportPlanItemEntity>> items(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID planId) { return service.items(actorId, planId); }
    @PostMapping("/plans/{planId}/approve")
    public Mono<ImportPlanView> approve(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId, @PathVariable UUID planId,
        @Valid @RequestBody ApproveImportPlanRequest request) { return service.approve(actorId, planId, request); }
    @PatchMapping("/plans/{planId}/items/{itemId}")
    public Mono<ImportPlanItemEntity> updateItem(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId, @PathVariable UUID planId,
        @PathVariable UUID itemId, @Valid @RequestBody UpdateImportPlanItemRequest request) {
        return service.updateItem(actorId, planId, itemId, request);
    }
}
