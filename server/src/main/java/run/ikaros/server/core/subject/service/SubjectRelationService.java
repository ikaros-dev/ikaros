package run.ikaros.server.core.subject.service;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.SubjectRelation;
import run.ikaros.api.store.enums.SubjectRelationType;
import run.ikaros.server.store.entity.SubjectRelationEntity;

public interface SubjectRelationService {
    Flux<SubjectRelation> findAllBySubjectId(UUID subjectId);

    Mono<SubjectRelation> findBySubjectIdAndType(UUID subjectId, SubjectRelationType relationType);

    Mono<SubjectRelationEntity> saveEntity(SubjectRelationEntity entity);

    Mono<SubjectRelation> createSubjectRelation(SubjectRelation subjectRelation);

    Mono<SubjectRelation> removeSubjectRelation(SubjectRelation subjectRelation);

}
