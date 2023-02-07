package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.CollectionEntity;

public interface CollectionRepository extends R2dbcRepository<CollectionEntity, Long> {
    Mono<CollectionEntity> findByUserIdAndSubjectId(Long userId, Long subjectId);
}
