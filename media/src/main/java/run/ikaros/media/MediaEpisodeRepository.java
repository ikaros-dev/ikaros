package run.ikaros.media;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MediaEpisodeRepository extends ReactiveCrudRepository<MediaEpisodeEntity, UUID> {
    Flux<MediaEpisodeEntity> findAllByOwnerIdAndSeasonIdOrderByEpisodeNumberAsc(UUID ownerId, UUID seasonId);
    Flux<MediaEpisodeEntity> findAllByOwnerIdAndSubjectIdOrderByEpisodeNumberAsc(UUID ownerId, UUID subjectId);

    @Query("select coalesce(max(episode_number), 0) from media_episode where owner_id = :ownerId and season_id = :seasonId")
    Mono<Integer> maxEpisodeNumber(UUID ownerId, UUID seasonId);

    @Modifying
    @Query("update media_episode set episode_number = episode_number + :offset, updated_at = current_timestamp where owner_id = :ownerId and season_id = :seasonId")
    Mono<Integer> shiftEpisodeNumbers(UUID ownerId, UUID seasonId, int offset);

    @Modifying
    @Query("update media_episode set episode_number = :episodeNumber, updated_at = current_timestamp where owner_id = :ownerId and id = :episodeId")
    Mono<Integer> updateEpisodeNumber(UUID ownerId, UUID episodeId, int episodeNumber);
}
