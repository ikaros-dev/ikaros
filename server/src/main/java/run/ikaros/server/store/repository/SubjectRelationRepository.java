package run.ikaros.server.store.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.SubjectRelationType;
import run.ikaros.server.store.entity.SubjectRelationEntity;

public interface SubjectRelationRepository extends R2dbcRepository<SubjectRelationEntity, UUID> {
    Flux<SubjectRelationEntity> findAllBySubjectIdAndRelationType(UUID subjectId,
                                                                  SubjectRelationType relationType);

    Flux<SubjectRelationEntity> findAllBySubjectId(UUID subjectId);

    Mono<Void> deleteBySubjectIdAndRelationTypeAndRelationSubjectId(
        UUID subjectId, SubjectRelationType relationType, UUID relationSubjectId);
}
