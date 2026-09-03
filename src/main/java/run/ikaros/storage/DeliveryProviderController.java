package run.ikaros.storage;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.IfMatchVersion;
import run.ikaros.task.BackgroundTask;

@RestController
@RequestMapping("/api/admin/delivery-providers")
public class DeliveryProviderController {
    private final DeliveryProviderService service;
    private final DeliveryProviderOperationsService operations;
    public DeliveryProviderController(DeliveryProviderService service, DeliveryProviderOperationsService operations) { this.service = service; this.operations = operations; }
    @GetMapping public Flux<DeliveryProviderView> list() { return service.list(); }
    @PostMapping public Mono<ResponseEntity<DeliveryProviderView>> create(
        @RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey,
        @Valid @RequestBody DeliveryProviderWriteRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return Mono.error(new IllegalArgumentException("缺少 Idempotency-Key"));
        return service.create(request, idempotencyKey).map(view -> ResponseEntity.created(URI.create("/api/admin/delivery-providers/" + view.id())).body(view));
    }
    @GetMapping("/{providerId}") public Mono<DeliveryProviderView> get(@PathVariable UUID providerId) { return service.get(providerId); }
    @PatchMapping("/{providerId}") public Mono<ResponseEntity<DeliveryProviderView>> update(@PathVariable UUID providerId,
        @RequestHeader(value="If-Match", required=false) String ifMatch, @Valid @RequestBody DeliveryProviderWriteRequest request) {
        return service.update(providerId, request, IfMatchVersion.parse(ifMatch))
            .map(view -> ResponseEntity.ok().eTag(IfMatchVersion.etag(view.version())).body(view));
    }
    @PostMapping("/{providerId}/probe") public Mono<ResponseEntity<BackgroundTask>> probe(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey, @PathVariable UUID providerId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return Mono.error(new IllegalArgumentException("缺少 Idempotency-Key"));
        return operations.probe(providerId, actorId, idempotencyKey).map(task -> accepted(task));
    }
    @PostMapping("/{providerId}/rotate-signing-key") public Mono<ResponseEntity<Void>> rotate(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey, @PathVariable UUID providerId,
        @RequestBody(required=false) RotateDeliverySigningKeyRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return Mono.error(new IllegalArgumentException("缺少 Idempotency-Key"));
        return operations.rotate(providerId, actorId, idempotencyKey, request).map(task -> ResponseEntity.accepted()
            .header("Location", "/api/background-tasks/" + task.id()).build());
    }
    private ResponseEntity<BackgroundTask> accepted(BackgroundTask task) { return ResponseEntity.accepted()
        .header("Location", "/api/background-tasks/" + task.id()).body(task); }
}
