package run.ikaros.server.store.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.EpisodeCollectionEntity;

public interface EpisodeCollectionRepository
    extends R2dbcRepository<EpisodeCollectionEntity, UUID> {

    Mono<EpisodeCollectionEntity> findByUserIdAndEpisodeId(UUID userId, UUID episodeId);

    Flux<EpisodeCollectionEntity> findAllByUserIdAndSubjectId(UUID userId, UUID subjectId);

}
