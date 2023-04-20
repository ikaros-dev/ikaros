package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.SubjectRelationEntity;

public interface SubjectRelationRepository extends R2dbcRepository<SubjectRelationEntity, Long> {
    Flux<SubjectRelationEntity> findAllBySubjectIdAndRelationType(Long subjectId,
                                                                  Integer relationType);

    Flux<SubjectRelationEntity> findAllBySubjectId(Long subjectId);

    Mono<Void> deleteBySubjectIdAndRelationTypeAndRelationSubjectId(Long subjectId,
                                                                    Integer relationType,
                                                                    Long relationSubjectId);
}
