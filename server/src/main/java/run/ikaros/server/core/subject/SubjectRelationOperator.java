package run.ikaros.server.core.subject;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.SubjectRelation;
import run.ikaros.api.core.subject.SubjectRelationOperate;
import run.ikaros.api.store.enums.SubjectRelationType;
import run.ikaros.server.core.subject.service.SubjectRelationService;

@Slf4j
@Component
public class SubjectRelationOperator implements SubjectRelationOperate {
    private final SubjectRelationService subjectRelationService;

    public SubjectRelationOperator(SubjectRelationService subjectRelationService) {
        this.subjectRelationService = subjectRelationService;
    }

    @Override
    public Flux<SubjectRelation> findAllBySubjectId(UUID subjectId) {
        return subjectRelationService.findAllBySubjectId(subjectId);
    }

    @Override
    public Mono<SubjectRelation> findBySubjectIdAndType(UUID subjectId,
                                                        SubjectRelationType relationType) {
        return subjectRelationService.findBySubjectIdAndType(subjectId, relationType);
    }

    @Override
    public Mono<SubjectRelation> createSubjectRelation(SubjectRelation subjectRelation) {
        return subjectRelationService.createSubjectRelation(subjectRelation);
    }

}
