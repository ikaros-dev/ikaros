package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.EpisodeFileEntity;

public interface EpisodeFileRepository extends R2dbcRepository<EpisodeFileEntity, Long> {
    Mono<Boolean> existsByEpisodeIdAndFileId(Long episodeId, Long fileId);

    Mono<Boolean> existsByEpisodeId(Long episodeId);

    Mono<Boolean> deleteByEpisodeIdAndFileId(Long episodeId, Long fileId);

    Flux<EpisodeFileEntity> findAllByEpisodeId(Long episodeId);

    Mono<Long> deleteAllByFileId(Long fileId);

}
