package run.ikaros.server.core.collection;

import java.util.UUID;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.collection.vo.FindCollectionCondition;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.api.wrap.PagingWrap;

public interface CollectionService {
    Mono<CollectionType> findTypeBySubjectId(UUID subjectId);

    Mono<PagingWrap> listCollectionsByCondition(FindCollectionCondition condition);
}
