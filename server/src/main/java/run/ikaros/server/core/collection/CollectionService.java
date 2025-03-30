package run.ikaros.server.core.collection;

import reactor.core.publisher.Mono;
import run.ikaros.api.core.collection.vo.FindCollectionCondition;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.api.wrap.PagingWrap;

public interface CollectionService {
    Mono<CollectionType> findTypeBySubjectId(Long subjectId);

    Mono<PagingWrap> listCollectionsByCondition(FindCollectionCondition condition);
}
