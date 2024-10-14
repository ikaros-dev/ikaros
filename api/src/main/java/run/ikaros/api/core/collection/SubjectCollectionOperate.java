package run.ikaros.api.core.collection;

import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.api.wrap.PagingWrap;

public interface SubjectCollectionOperate extends CollectionOperate {

    Mono<Void> collect(Long userId, Long subjectId,
                       CollectionType type, Boolean isPrivate);

    Mono<SubjectCollection> findCollection(Long userId, Long subjectId);

    Mono<PagingWrap<SubjectCollection>> findCollections(Long userId, Integer page,
                                                        Integer size);

    Mono<PagingWrap<SubjectCollection>> findCollections(Long userId, Integer page,
                                                        Integer size,
                                                        CollectionType type,
                                                        Boolean isPrivate);

    Mono<Void> updateMainEpisodeProgress(Long userId, Long subjectId, Integer progress);
}
