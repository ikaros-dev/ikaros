package run.ikaros.storage.api;

import java.time.Instant;
import java.util.UUID;

public record DeliveryGrantView(UUID id, UUID attachmentId, String token, String method,
                                Instant expiresAt, Long rangeStart, Long rangeEnd,
                                DeliveryGrantRevocationLevel revocationLevel, Long version) {
    public DeliveryGrantView(UUID id, UUID attachmentId, String token, String method, Instant expiresAt,
                             Long rangeStart, Long rangeEnd, DeliveryGrantRevocationLevel revocationLevel) {
        this(id, attachmentId, token, method, expiresAt, rangeStart, rangeEnd, revocationLevel, null);
    }
}
