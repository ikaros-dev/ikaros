package run.ikaros.server.store.repository;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
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

    @Query("""
        select count(*) from subject_collection sc
        join subject s on s.id = sc.subject_id
        where s.delete_status = false or s.delete_status is null
        """)
    Mono<Long> countActive();

    @Query("""
        select count(*) from subject_collection sc
        join subject s on s.id = sc.subject_id
        where sc.type = $1
          and (s.delete_status = false or s.delete_status is null)
        """)
    Mono<Long> countActiveByType(CollectionType type);

    Mono<Void> removeAllBySubjectId(UUID subjectId);
}
