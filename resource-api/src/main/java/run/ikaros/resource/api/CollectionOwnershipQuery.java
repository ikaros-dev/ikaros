package run.ikaros.resource.api;

import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Collection ownership capability for modules that need to authorize access
 * without depending on Collection persistence internals.
 */
public interface CollectionOwnershipQuery {
    Mono<Void> requireOwned(UUID ownerId, UUID collectionId);
}
