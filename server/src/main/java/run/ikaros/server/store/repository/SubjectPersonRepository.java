package run.ikaros.server.store.repository;

import java.util.UUID;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.SubjectPersonEntity;

public interface SubjectPersonRepository
    extends BaseRepository<SubjectPersonEntity> {
    Mono<SubjectPersonEntity> findBySubjectIdAndPersonId(UUID subjectId, UUID personId);
}
