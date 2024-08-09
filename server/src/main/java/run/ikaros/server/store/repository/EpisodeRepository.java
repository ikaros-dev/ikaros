package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.server.store.entity.EpisodeEntity;

public interface EpisodeRepository extends R2dbcRepository<EpisodeEntity, Long> {
    Flux<EpisodeEntity> findAllBySubjectId(Long subjectId);

    Mono<Long> countBySubjectId(Long subjectId);

    Flux<EpisodeEntity> findAllBySubjectIdOrderByGroupDescSequenceAscAirTimeAscCreateTimeAsc(
        Long subjectId);

    Mono<Void> deleteAllBySubjectId(Long subjectId);

    Mono<EpisodeEntity> findBySubjectIdAndGroupAndSequence(Long subjectId, EpisodeGroup group,
                                                           Float sequence);
}
