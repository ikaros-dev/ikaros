package run.ikaros.server.core.collection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.collection.SubjectCollection;
import run.ikaros.api.core.collection.SubjectCollectionOperate;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.api.wrap.PagingWrap;

@Slf4j
@Component
public class SubjectCollectionOperator implements SubjectCollectionOperate {
    private final SubjectCollectionService service;

    public SubjectCollectionOperator(SubjectCollectionService service) {
        this.service = service;
    }

    @Override
    public Mono<Void> collect(Long userId, Long subjectId, CollectionType type, Boolean isPrivate) {
        return service.collect(userId, subjectId, type, isPrivate);
    }

    @Override
    public Mono<Void> unCollect(Long userId, Long subjectId) {
        return service.unCollect(userId, subjectId);
    }

    @Override
    public Mono<SubjectCollection> findCollection(Long userId, Long subjectId) {
        return service.findCollection(userId, subjectId);
    }

    @Override
    public Mono<PagingWrap<SubjectCollection>> findUserCollections(
        Long userId, Integer page, Integer size) {
        return service.findUserCollections(userId, page, size);
    }

    @Override
    public Mono<PagingWrap<SubjectCollection>> findUserCollections(Long userId, Integer page,
                                                                   Integer size,
                                                                   CollectionType type,
                                                                   Boolean isPrivate) {
        return service.findUserCollections(userId, page, size, type, isPrivate);
    }
}
