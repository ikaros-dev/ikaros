package run.ikaros.resource.api;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Provides an explicitly allow-listed projection for the Search module. */
public interface ResourceSearchProjectionQuery {
    Mono<ResourceSearchProjection> find(UUID resourceId);

    /** Returns the current projectable Resource projections for a full rebuild. */
    Flux<ResourceSearchProjection> findAll();
}
