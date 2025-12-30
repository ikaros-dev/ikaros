package run.ikaros.server.store.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.server.store.entity.EpisodeEntity;

public interface EpisodeRepository extends R2dbcRepository<EpisodeEntity, UUID> {
    Flux<EpisodeEntity> findAllBySubjectId(UUID subjectId);

    @Query(value = "select id from episode where subject_id = :subjectId")
    Flux<Long> findAllIdBySubjectId(UUID subjectId);

    @Query(value = "select count(id) from episode where subject_id = :subjectId")
    Mono<Long> countBySubjectId(UUID subjectId);

    Flux<EpisodeEntity> findAllBySubjectIdOrderByGroupDescSequenceAscAirTimeAscCreateTimeAsc(
        UUID subjectId);

    Mono<Void> deleteAllBySubjectId(UUID subjectId);

    Mono<Void> deleteBySubjectIdAndGroupAndSequenceAndName(
        UUID subjectId, EpisodeGroup group, Float sequence, String name);

    Flux<EpisodeEntity> findBySubjectIdAndGroupAndSequence(UUID subjectId, EpisodeGroup group,
                                                           Float sequence);

    Mono<EpisodeEntity> findBySubjectIdAndGroupAndSequenceAndName(
        UUID subjectId, EpisodeGroup group, Float sequence, String name);
}
