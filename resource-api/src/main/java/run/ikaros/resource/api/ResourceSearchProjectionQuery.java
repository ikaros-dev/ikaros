package run.ikaros.resource.api;

import java.util.UUID;
import reactor.core.publisher.Mono;

/** Provides an explicitly allow-listed projection for the Search module. */
public interface ResourceSearchProjectionQuery {
    Mono<ResourceSearchProjection> find(UUID resourceId);
}
