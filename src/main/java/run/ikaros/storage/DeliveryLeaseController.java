package run.ikaros.storage;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import org.springframework.http.ResponseEntity;
import run.ikaros.common.IfMatchVersion;

@RestController
@RequestMapping("/api/attachments/{attachmentId}/delivery-leases")
public class DeliveryLeaseController {
    private final DeliveryLeaseService service;
    public DeliveryLeaseController(DeliveryLeaseService service) { this.service = service; }

    @PostMapping
    public Mono<DeliveryLeaseView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID attachmentId, @Valid @RequestBody DeliveryLeaseRequest request) {
        return service.create(actorId, attachmentId, request);
    }

    @PostMapping("/{leaseId}/actions/renew")
    public Mono<ResponseEntity<DeliveryLeaseView>> renew(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID leaseId, @RequestHeader(value = "If-Match", required = false) String ifMatch,
        @RequestBody(required = false) DeliveryLeaseRequest request) {
        return service.renew(actorId, leaseId, request == null ? null : request.ttlSeconds(), IfMatchVersion.parse(ifMatch))
            .map(view -> ResponseEntity.ok().eTag(IfMatchVersion.etag(view.version())).body(view));
    }

    @PostMapping("/{leaseId}/actions/release")
    public Mono<Void> release(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID leaseId, @RequestHeader(value = "If-Match", required = false) String ifMatch) {
        return service.release(actorId, leaseId, IfMatchVersion.parse(ifMatch));
    }
}
