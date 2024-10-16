package run.ikaros.server.core.collection;

import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.CollectionType;

public interface CollectionService {
    Mono<CollectionType> findTypeBySubjectId(Long subjectId);
}
