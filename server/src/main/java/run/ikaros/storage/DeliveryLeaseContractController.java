package run.ikaros.storage;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import run.ikaros.common.IfMatchVersion;

/** Top-level lease mutation routes defined by the v2 delivery contract. */
@RestController
@RequestMapping("/api/delivery-leases")
public class DeliveryLeaseContractController {
    private final DeliveryLeaseService service;
    public DeliveryLeaseContractController(DeliveryLeaseService service) { this.service = service; }

    @PostMapping("/{leaseId}/renew")
    public Mono<ResponseEntity<DeliveryLeaseView>> renew(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID leaseId, @RequestHeader(value="If-Match", required=false) String ifMatch,
        @RequestBody(required=false) DeliveryLeaseRequest request) {
        return service.renew(actorId, leaseId, request == null ? null : request.ttlSeconds(), IfMatchVersion.parse(ifMatch))
            .map(view -> ResponseEntity.ok().eTag(IfMatchVersion.etag(view.version())).body(view));
    }

    @DeleteMapping("/{leaseId}")
    public Mono<ResponseEntity<Void>> release(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID leaseId, @RequestHeader(value="If-Match", required=false) String ifMatch) {
        return service.release(actorId, leaseId, IfMatchVersion.parse(ifMatch))
            .thenReturn(ResponseEntity.noContent().build());
    }
}
