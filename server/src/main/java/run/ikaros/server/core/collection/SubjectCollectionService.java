package run.ikaros.server.core.collection;

import reactor.core.publisher.Mono;
import run.ikaros.api.core.collection.SubjectCollection;
import run.ikaros.api.store.enums.CollectionType;

public interface SubjectCollectionService {
    Mono<SubjectCollection> collect(Long userId, Long subjectId, CollectionType type);

    Mono<SubjectCollection> unCollect(Long userId, Long subjectId);
}
