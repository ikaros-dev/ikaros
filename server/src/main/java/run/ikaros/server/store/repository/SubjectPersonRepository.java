package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.SubjectPersonEntity;

public interface SubjectPersonRepository
    extends R2dbcRepository<SubjectPersonEntity, Long> {
    Mono<SubjectPersonEntity> findBySubjectIdAndPersonId(Long subjectId, Long personId);
}
