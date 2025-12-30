package run.ikaros.server.core.collection;

import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.collection.SubjectCollection;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.api.wrap.PagingWrap;

public interface SubjectCollectionService {

    @Transactional
    Mono<Void> collect(SubjectCollection subjectCollection);

    @Transactional
    Mono<Void> collect(UUID userId, UUID subjectId,
                       CollectionType type, Boolean isPrivate);

    /**
     * collect.
     *
     * @see #collect(Long, Long, CollectionType, Boolean)
     */
    Mono<Void> collect(UUID userId, UUID subjectId, CollectionType type);

    @Transactional
    Mono<Void> unCollect(UUID userId, UUID subjectId);

    /**
     * Find subject collection with userId and subjectId.
     *
     * @param userId    user id
     * @param subjectId subject id
     * @return subject collection or null
     */
    Mono<SubjectCollection> findCollection(UUID userId, UUID subjectId);

    Mono<PagingWrap<SubjectCollection>> findCollections(UUID userId, Integer page,
                                                        Integer size);

    Mono<PagingWrap<SubjectCollection>> findCollections(UUID userId, Integer page,
                                                        Integer size,
                                                        CollectionType type,
                                                        Boolean isPrivate);

    Mono<Void> updateMainEpisodeProgress(UUID userId, UUID subjectId, Integer progress);
}
