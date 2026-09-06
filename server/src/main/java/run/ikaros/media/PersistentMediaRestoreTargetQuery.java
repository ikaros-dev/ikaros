package run.ikaros.media;

import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.media.api.MediaRestoreTargetQuery;

/** Media-owned implementation of the Restore target capability. */
@Service
final class PersistentMediaRestoreTargetQuery implements MediaRestoreTargetQuery {
    private final MediaSeasonRepository seasons;
    private final MediaEpisodeRepository episodes;

    PersistentMediaRestoreTargetQuery(MediaSeasonRepository seasons, MediaEpisodeRepository episodes) {
        this.seasons = seasons;
        this.episodes = episodes;
    }

    @Override
    public Mono<Void> requireOwnedSeason(UUID actorId, UUID seasonId) {
        return seasons.findById(seasonId)
            .filter(season -> season.ownerId().equals(actorId))
            .switchIfEmpty(Mono.error(new NotFoundException("Season 不存在或无权访问")))
            .then();
    }

    @Override
    public Flux<UUID> findOwnedEpisodeResourceIds(UUID actorId, UUID seasonId) {
        return episodes.findAllByOwnerIdAndSeasonIdOrderByEpisodeNumberAsc(actorId, seasonId)
            .map(MediaEpisodeEntity::resourceId);
    }
}
