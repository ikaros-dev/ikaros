package run.ikaros.server.store.repository;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.server.store.entity.SubjectCollectionEntity;

public interface SubjectCollectionRepository
    extends BaseRepository<SubjectCollectionEntity> {
    Mono<SubjectCollectionEntity> findByUserIdAndSubjectId(UUID userId, UUID subjectId);

    Flux<SubjectCollectionEntity> findAllByUserId(UUID userId, Pageable pageable);

    Mono<Long> countAllByUserId(UUID userId);

    Mono<Long> countByType(CollectionType type);

    Mono<Void> removeAllBySubjectId(UUID subjectId);
}
