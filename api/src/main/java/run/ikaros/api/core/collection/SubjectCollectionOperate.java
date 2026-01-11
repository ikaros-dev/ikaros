package run.ikaros.api.core.collection;

import java.util.UUID;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.api.wrap.PagingWrap;

public interface SubjectCollectionOperate extends CollectionOperate {

    Mono<Void> collect(UUID userId, UUID subjectId,
                       CollectionType type, Boolean isPrivate);

    Mono<SubjectCollection> findCollection(UUID userId, UUID subjectId);

    Mono<PagingWrap<SubjectCollection>> findCollections(UUID userId, Integer page,
                                                        Integer size);

    Mono<PagingWrap<SubjectCollection>> findCollections(UUID userId, Integer page,
                                                        Integer size,
                                                        CollectionType type,
                                                        Boolean isPrivate);

    Mono<Void> updateMainEpisodeProgress(UUID userId, UUID subjectId, Integer progress);
}
