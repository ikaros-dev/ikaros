package run.ikaros.server.core.collection;

import java.util.UUID;
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
    public Mono<Void> collect(UUID userId, UUID subjectId, CollectionType type, Boolean isPrivate) {
        return service.collect(userId, subjectId, type, isPrivate);
    }

    @Override
    public Mono<SubjectCollection> findCollection(UUID userId, UUID subjectId) {
        return service.findCollection(userId, subjectId);
    }

    @Override
    public Mono<PagingWrap<SubjectCollection>> findCollections(
        UUID userId, Integer page, Integer size) {
        return service.findCollections(userId, page, size);
    }

    @Override
    public Mono<PagingWrap<SubjectCollection>> findCollections(UUID userId, Integer page,
                                                               Integer size,
                                                               CollectionType type,
                                                               Boolean isPrivate) {
        return service.findCollections(userId, page, size, type, isPrivate);
    }

    @Override
    public Mono<Void> updateMainEpisodeProgress(UUID userId, UUID subjectId, Integer progress) {
        return service.updateMainEpisodeProgress(userId, subjectId, progress);
    }
}
