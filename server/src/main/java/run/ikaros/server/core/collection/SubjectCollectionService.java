package run.ikaros.server.core.collection;

import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.collection.SubjectCollection;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.api.wrap.PagingWrap;

public interface SubjectCollectionService {

    @Transactional
    Mono<Void> collect(Long userId, Long subjectId,
                       CollectionType type, Boolean isPrivate);

    @Transactional
    Mono<Void> collect(Long userId, Long subjectId, CollectionType type);

    @Transactional
    Mono<Void> unCollect(Long userId, Long subjectId);

    Mono<SubjectCollection> findCollection(Long userId, Long subjectId);

    Mono<PagingWrap<SubjectCollection>> findUserCollections(Long userId, Integer page,
                                                            Integer size);

    Mono<PagingWrap<SubjectCollection>> findUserCollections(Long userId, Integer page,
                                                            Integer size,
                                                            CollectionType type,
                                                            Boolean isPrivate);
}
