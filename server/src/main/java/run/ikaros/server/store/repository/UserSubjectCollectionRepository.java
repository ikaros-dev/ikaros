package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.UserSubjectCollectionEntity;

public interface UserSubjectCollectionRepository
    extends R2dbcRepository<UserSubjectCollectionEntity, Long> {
    Mono<UserSubjectCollectionEntity> findByUserIdAndSubjectId(Long userId, Long subjectId);
}
