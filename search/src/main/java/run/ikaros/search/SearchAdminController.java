package run.ikaros.search;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.resource.api.ResourceSearchProjectionQuery;

/** Admin operations for observing and repairing the rebuildable search projection. */
@RestController
@RequestMapping("/api/admin/search")
public class SearchAdminController {
    private static final String DEFAULT_PROJECTOR_VERSION = "resource-v1";
    private final SearchRebuildService rebuildService;
    private final ResourceSearchProjectionQuery source;
    private final SearchReconciliationService reconciliationService;

    public SearchAdminController(SearchRebuildService rebuildService,
                                 ResourceSearchProjectionQuery source,
                                 SearchReconciliationService reconciliationService) {
        this.rebuildService = rebuildService;
        this.source = source;
        this.reconciliationService = reconciliationService;
    }

    @PostMapping("/rebuild")
    public Mono<SearchRebuildResult> rebuild(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
                                             @RequestBody(required = false) SearchRebuildRequest request) {
        String projectorVersion = request == null || request.projectorVersion() == null
            ? DEFAULT_PROJECTOR_VERSION : request.projectorVersion();
        return rebuildService.rebuild(source.findAll()
            .map(projection -> new SearchProjectionInput(projection.resourceId(), projection.sourceVersion(),
                projection.fields())), projectorVersion);
    }

    @GetMapping("/projection-failures")
    public Flux<ProjectionFailureView> failures(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId) {
        return reconciliationService.pendingFailures();
    }

    @PostMapping("/projection-failures/{failureId}/retry")
    public Mono<ProjectionFailureView> retry(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
                                             @PathVariable UUID failureId) {
        return reconciliationService.retry(failureId);
    }

    public record SearchRebuildRequest(@JsonProperty("projector_version") String projectorVersion) { }
}
