package run.ikaros.resource;

import java.util.UUID;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.resource.api.ResourceOwnershipQuery;

@Service
public class DefaultResourceOwnershipQuery implements ResourceOwnershipQuery {
    private final ResourceRepository resources;
    private final UserResourceRepository userResources;

    public DefaultResourceOwnershipQuery(ResourceRepository resources) {
        this.resources = resources;
        this.userResources = null;
    }

    @Autowired
    public DefaultResourceOwnershipQuery(ResourceRepository resources, UserResourceRepository userResources) {
        this.resources = resources;
        this.userResources = userResources;
    }

    @Override
    public Mono<Void> requireOwned(UUID ownerId, UUID resourceId) {
        return resources.findByIdAndOwnerId(resourceId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("资源不存在或无权访问")))
            .then();
    }

    @Override
    public Mono<Void> requireReadable(UUID userId, UUID resourceId) {
        return requireRole(userId, resourceId, List.of("OWNER", "EDITOR", "VIEWER"));
    }

    @Override
    public Mono<Void> requireWritable(UUID userId, UUID resourceId) {
        return requireRole(userId, resourceId, List.of("OWNER", "EDITOR"));
    }

    private Mono<Void> requireRole(UUID userId, UUID resourceId, List<String> roles) {
        if (userResources == null) return requireOwned(userId, resourceId);
        return userResources.findByResourceIdAndUserIdAndRoleIn(resourceId, userId, roles)
            .switchIfEmpty(Mono.error(new NotFoundException("资源不存在或无权访问")))
            .then();
    }
}
