package run.ikaros.storage.api;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface DeliveryGrantService {
    Mono<DeliveryGrantView> issue(UUID actorId, UUID attachmentId, DeliveryGrantRequest request);
    Mono<UUID> authorize(UUID actorId, UUID attachmentId, String token, String range);
    Mono<Void> revoke(UUID actorId, UUID grantId);
    Mono<Void> revoke(UUID actorId, UUID grantId, long expectedVersion);
}
