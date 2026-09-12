package run.ikaros.collection;

import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.resource.api.CollectionOwnershipQuery;

@Service
public class DefaultCollectionOwnershipQuery implements CollectionOwnershipQuery {
    private final CollectionRepository collections;

    public DefaultCollectionOwnershipQuery(CollectionRepository collections) {
        this.collections = collections;
    }

    @Override
    public Mono<Void> requireOwned(UUID ownerId, UUID collectionId) {
        return collections.findByIdAndOwnerId(collectionId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("集合不存在或无权访问")))
            .then();
    }
}
