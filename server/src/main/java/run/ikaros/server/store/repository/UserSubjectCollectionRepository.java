package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.SubjectCollectionEntity;

public interface UserSubjectCollectionRepository
    extends R2dbcRepository<SubjectCollectionEntity, Long> {
    Mono<SubjectCollectionEntity> findByUserIdAndSubjectId(Long userId, Long subjectId);
}
