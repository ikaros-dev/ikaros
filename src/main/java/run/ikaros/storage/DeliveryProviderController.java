package run.ikaros.storage;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.IfMatchVersion;

@RestController
@RequestMapping("/api/v2/admin/delivery-providers")
public class DeliveryProviderController {
    private final DeliveryProviderService service;
    public DeliveryProviderController(DeliveryProviderService service) { this.service = service; }
    @GetMapping public Flux<DeliveryProviderView> list() { return service.list(); }
    @PostMapping public Mono<ResponseEntity<DeliveryProviderView>> create(
        @RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey,
        @Valid @RequestBody DeliveryProviderWriteRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return Mono.error(new IllegalArgumentException("缺少 Idempotency-Key"));
        return service.create(request).map(view -> ResponseEntity.created(URI.create("/api/v2/admin/delivery-providers/" + view.id())).body(view));
    }
    @GetMapping("/{providerId}") public Mono<DeliveryProviderView> get(@PathVariable UUID providerId) { return service.get(providerId); }
    @PatchMapping("/{providerId}") public Mono<ResponseEntity<DeliveryProviderView>> update(@PathVariable UUID providerId,
        @RequestHeader(value="If-Match", required=false) String ifMatch, @Valid @RequestBody DeliveryProviderWriteRequest request) {
        return service.update(providerId, request, IfMatchVersion.parse(ifMatch))
            .map(view -> ResponseEntity.ok().eTag(IfMatchVersion.etag(view.version())).body(view));
    }
}
