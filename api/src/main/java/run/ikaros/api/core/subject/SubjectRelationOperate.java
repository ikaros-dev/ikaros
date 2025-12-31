package run.ikaros.api.core.subject;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;
import run.ikaros.api.store.enums.SubjectRelationType;

public interface SubjectRelationOperate extends AllowPluginOperate {
    Flux<SubjectRelation> findAllBySubjectId(UUID subjectId);

    Mono<SubjectRelation> findBySubjectIdAndType(UUID subjectId, SubjectRelationType relationType);

    Mono<SubjectRelation> createSubjectRelation(SubjectRelation subjectRelation);

}
