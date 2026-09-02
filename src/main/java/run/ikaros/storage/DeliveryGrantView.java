package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;

public record DeliveryGrantView(UUID id, UUID attachmentId, String token, String method,
                                Instant expiresAt, Long rangeStart, Long rangeEnd,
                                DeliveryGrantRevocationLevel revocationLevel) {}
