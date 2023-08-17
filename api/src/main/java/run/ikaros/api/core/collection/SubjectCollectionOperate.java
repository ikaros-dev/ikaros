package run.ikaros.api.core.collection;

import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.api.wrap.PagingWrap;

public interface SubjectCollectionOperate extends CollectionOperate {

    Mono<Void> collect(Long userId, Long subjectId,
                       CollectionType type, Boolean isPrivate);

    Mono<Void> unCollect(Long userId, Long subjectId);

    Mono<SubjectCollection> findCollection(Long userId, Long subjectId);

    Mono<PagingWrap<SubjectCollection>> findUserCollections(Long userId, Integer page,
                                                            Integer size);

}
