package run.ikaros.storage;

import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import org.springframework.http.ResponseEntity;
import run.ikaros.common.IfMatchVersion;

@RestController
@RequestMapping("/api/storage/restore-operations")
public class StorageRestoreReconciliationController {
    private final StorageRestoreReconciliationService service;
    public StorageRestoreReconciliationController(StorageRestoreReconciliationService service) { this.service = service; }
    @PostMapping("/{operationId}/actions/reconcile")
    public Mono<ResponseEntity<StorageRestoreOperationView>> reconcile(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID operationId) { return service.reconcile(actorId, operationId)
        .map(view -> ResponseEntity.ok().eTag(IfMatchVersion.etag(view.version())).body(view)); }
}
