package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;

public record DeliveryLeaseView(UUID id, UUID attachmentId, UUID blobId, Instant leaseExpiresAt,
                                Instant lastHeartbeatAt, boolean active, Long version) {
    public DeliveryLeaseView(UUID id, UUID attachmentId, UUID blobId, Instant leaseExpiresAt,
                             Instant lastHeartbeatAt, boolean active) {
        this(id, attachmentId, blobId, leaseExpiresAt, lastHeartbeatAt, active, null);
    }
}
