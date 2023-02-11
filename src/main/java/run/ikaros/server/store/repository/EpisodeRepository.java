package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.EpisodeEntity;

public interface EpisodeRepository extends R2dbcRepository<EpisodeEntity, Long> {
    Flux<EpisodeEntity> findBySubjectId(Long subjectId);

    Mono<Void> deleteAllBySubjectId(Long subjectId);
}
