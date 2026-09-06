package run.ikaros.media.api;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Public Media capability used by Storage to resolve media-owned restore targets. */
public interface MediaRestoreTargetQuery {
    Mono<Void> requireOwnedSeason(UUID actorId, UUID seasonId);

    Flux<UUID> findOwnedEpisodeResourceIds(UUID actorId, UUID seasonId);
}
