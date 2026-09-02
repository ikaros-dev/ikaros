package run.ikaros.storage;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface DeliveryLeaseService {
    Mono<DeliveryLeaseView> create(UUID actorId, UUID attachmentId, DeliveryLeaseRequest request);
    Mono<DeliveryLeaseView> renew(UUID actorId, UUID leaseId, Integer ttlSeconds);
    Mono<Void> release(UUID actorId, UUID leaseId);
    Mono<Boolean> protectsBlob(UUID blobId);
}
