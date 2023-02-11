package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.SubjectImageEntity;

public interface SubjectImageRepository extends R2dbcRepository<SubjectImageEntity, Long> {
    Mono<SubjectImageEntity> findBySubjectId(Long subjectId);

    Mono<Void> deleteBySubjectId(Long subjectId);
}
