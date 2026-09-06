package run.ikaros.storage;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import run.ikaros.common.IfMatchVersion;

@RestController
@RequestMapping({"/api/storage/restore-budget", "/api/admin/restore-budget-policy"})
public class StorageRestoreBudgetController {
    private final StorageRestoreBudgetService service;
    public StorageRestoreBudgetController(StorageRestoreBudgetService service) { this.service = service; }
    @GetMapping
    public Mono<StorageRestoreBudgetView> get(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId) { return service.get(); }
    @PutMapping
    public Mono<ResponseEntity<StorageRestoreBudgetView>> update(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @RequestHeader(value="If-Match", required=false) String ifMatch, @Valid @RequestBody StorageRestoreBudgetRequest request) {
        return service.update(request, IfMatchVersion.parse(ifMatch)).map(view -> ResponseEntity.ok()
            .eTag(IfMatchVersion.etag(view.version())).body(view)); }
}
