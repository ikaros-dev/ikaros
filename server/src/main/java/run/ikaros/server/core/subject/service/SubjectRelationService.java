package run.ikaros.server.core.subject.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.SubjectRelation;
import run.ikaros.api.store.enums.SubjectRelationType;

public interface SubjectRelationService {
    Flux<SubjectRelation> findAllBySubjectId(Long subjectId);

    Mono<SubjectRelation> findBySubjectIdAndType(Long subjectId, SubjectRelationType relationType);

    Mono<SubjectRelation> createSubjectRelation(SubjectRelation subjectRelation);

    Mono<SubjectRelation> removeSubjectRelation(SubjectRelation subjectRelation);

}
