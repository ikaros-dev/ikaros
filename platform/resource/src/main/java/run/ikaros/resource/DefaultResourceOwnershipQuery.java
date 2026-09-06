package run.ikaros.resource;

import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.resource.api.ResourceOwnershipQuery;

@Service
public class DefaultResourceOwnershipQuery implements ResourceOwnershipQuery {
    private final ResourceRepository resources;

    public DefaultResourceOwnershipQuery(ResourceRepository resources) {
        this.resources = resources;
    }

    @Override
    public Mono<Void> requireOwned(UUID ownerId, UUID resourceId) {
        return resources.findByIdAndOwnerId(resourceId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("资源不存在或无权访问")))
            .then();
    }
}
