package run.ikaros.storage;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface DeliveryLeaseService {
    Mono<DeliveryLeaseView> create(UUID actorId, UUID attachmentId, DeliveryLeaseRequest request);
    Mono<DeliveryLeaseView> create(UUID actorId, UUID attachmentId, DeliveryLeaseRequest request, UUID bindingId);
    Mono<DeliveryLeaseView> get(UUID actorId, UUID leaseId);
    Mono<DeliveryLeaseView> renew(UUID actorId, UUID leaseId, Integer ttlSeconds);
    Mono<DeliveryLeaseView> renew(UUID actorId, UUID leaseId, Integer ttlSeconds, long expectedVersion);
    Mono<Void> release(UUID actorId, UUID leaseId);
    Mono<Void> release(UUID actorId, UUID leaseId, long expectedVersion);
    Mono<Boolean> protectsBlob(UUID blobId);
}
