package run.ikaros.server.search.subject;

import java.time.OffsetDateTime;
import reactor.core.publisher.Mono;
import run.ikaros.api.search.subject.SubjectDoc;
import run.ikaros.server.store.entity.SubjectEntity;

public class ReactiveSubjectDocConverter {

    /**
     * Get {@link SubjectDoc} from {@link  SubjectEntity}.
     */
    public static Mono<SubjectDoc> fromEntity(SubjectEntity entity) {
        SubjectDoc subjectDoc = new SubjectDoc();
        subjectDoc.setName(entity.getName());
        subjectDoc.setNsfw(entity.getNsfw());
        subjectDoc.setSummary(entity.getSummary());
        subjectDoc.setInfobox(entity.getInfobox());
        subjectDoc.setType(entity.getType());
        subjectDoc.setNameCn(entity.getNameCn());
        subjectDoc.setAirTime(entity.getAirTime().toEpochSecond(OffsetDateTime.now().getOffset()));
        return Mono.just(subjectDoc);
    }

}
