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
import run.ikaros.common.IfMatchVersion;

@RestController
@RequestMapping({"/api/attachments/{attachmentId}/delivery-grants", "/api/v2/attachments/{attachmentId}/delivery-grants"})
public class DeliveryGrantController {
    private final DeliveryGrantService service;
    private final DeliveryLeaseService leases;
    private final BlobRepository blobs;
    private final MediaDeliveryBindingRepository bindings;
    private final DeliveryProviderRepository providers;

    public DeliveryGrantController(DeliveryGrantService service, DeliveryLeaseService leases, BlobRepository blobs,
                                   MediaDeliveryBindingRepository bindings, DeliveryProviderRepository providers) {
        this.service = service; this.leases = leases; this.blobs = blobs; this.bindings = bindings; this.providers = providers;
    }

    @PostMapping
    public Mono<DeliveryGrantContractView> issue(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID attachmentId, @Valid @RequestBody DeliveryGrantRequest request) {
        return service.issue(actorId, attachmentId, request).flatMap(grant ->
            (request != null && request.existingLeaseId() != null
                ? leases.get(actorId, request.existingLeaseId()).filter(lease -> lease.attachmentId().equals(attachmentId))
                : leases.create(actorId, attachmentId, new DeliveryLeaseRequest(grant.token(), request == null ? null : request.ttlSeconds())))
                .flatMap(lease -> bindings.findById(lease.bindingId())
                    .flatMap(binding -> providers.findByProviderKey(binding.deliveryProviderKey())
                        .zipWith(blobs.findById(lease.blobId()))
                        .map(providerAndBlob -> new DeliveryGrantContractView(grant.id(), grant.attachmentId(), lease.id(),
                            providerAndBlob.getT1().id(), grant.method(), "/api/v2/attachments/" + attachmentId
                                + "/content?delivery_grant=" + grant.token(), grant.expiresAt(),
                            binding.rangePolicy() != DeliveryBindingRangePolicy.UNSUPPORTED,
                            providerAndBlob.getT2().mediaType(), providerAndBlob.getT2().sizeBytes(), grant.revocationLevel())))));
    }

    @PostMapping("/{grantId}/actions/revoke")
    public Mono<Void> revoke(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID grantId, @RequestHeader(value = "If-Match", required = false) String ifMatch) {
        return service.revoke(actorId, grantId, IfMatchVersion.parse(ifMatch));
    }
}
