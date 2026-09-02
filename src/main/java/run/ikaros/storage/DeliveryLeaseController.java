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

@RestController
@RequestMapping("/api/v2/attachments/{attachmentId}/delivery-leases")
public class DeliveryLeaseController {
    private final DeliveryLeaseService service;
    public DeliveryLeaseController(DeliveryLeaseService service) { this.service = service; }

    @PostMapping
    public Mono<DeliveryLeaseView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID attachmentId, @Valid @RequestBody DeliveryLeaseRequest request) {
        return service.create(actorId, attachmentId, request);
    }

    @PostMapping("/{leaseId}/actions/renew")
    public Mono<DeliveryLeaseView> renew(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID leaseId, @RequestBody(required = false) DeliveryLeaseRequest request) {
        return service.renew(actorId, leaseId, request == null ? null : request.ttlSeconds());
    }

    @PostMapping("/{leaseId}/actions/release")
    public Mono<Void> release(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID leaseId) { return service.release(actorId, leaseId); }
}
