package run.ikaros.server.store.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.server.store.entity.SubjectCollectionEntity;

public interface SubjectCollectionRepository
    extends R2dbcRepository<SubjectCollectionEntity, Long> {
    Mono<SubjectCollectionEntity> findByUserIdAndSubjectId(Long userId, Long subjectId);

    Flux<SubjectCollectionEntity> findAllByUserId(Long userId, Pageable pageable);

    Mono<Long> countAllByUserId(Long userId);

    Mono<Long> countByType(CollectionType type);

    Mono<Void> removeAllBySubjectId(Long subjectId);
}
