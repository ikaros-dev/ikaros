package run.ikaros.resource.api;

import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Resource ownership capability for modules that need to authorize access
 * without depending on Resource persistence internals.
 */
public interface ResourceOwnershipQuery {
    Mono<Void> requireOwned(UUID ownerId, UUID resourceId);
}
