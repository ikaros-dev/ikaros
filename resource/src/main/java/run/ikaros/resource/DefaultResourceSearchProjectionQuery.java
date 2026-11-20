package run.ikaros.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.resource.api.ResourceSearchProjection;
import run.ikaros.resource.api.ResourceSearchProjectionQuery;
import run.ikaros.resource.api.ResourceStatus;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class DefaultResourceSearchProjectionQuery implements ResourceSearchProjectionQuery {
    private final ResourceRepository resources;
    private final ResourceTitleRepository titles;
    private final ResourceTagRepository tags;
    private final UserResourceRepository userResources;

    public DefaultResourceSearchProjectionQuery(ResourceRepository resources, ResourceTitleRepository titles,
                                                ResourceTagRepository tags) {
        this.resources = resources;
        this.titles = titles;
        this.tags = tags;
        this.userResources = null;
    }

    @Autowired
    public DefaultResourceSearchProjectionQuery(ResourceRepository resources, ResourceTitleRepository titles,
                                                ResourceTagRepository tags, UserResourceRepository userResources) {
        this.resources = resources;
        this.titles = titles;
        this.tags = tags;
        this.userResources = userResources;
    }

    @Override
    public Mono<ResourceSearchProjection> find(UUID resourceId) {
        return resources.findById(resourceId)
            .filter(resource -> resource.status() == ResourceStatus.ACTIVE.code())
            .flatMap(resource -> {
                Mono<java.util.List<ResourceTagEntity>> resourceTagsMono = userResources == null
                    ? Flux.<ResourceTagEntity>empty().collectList()
                    : ownerId(resource.id()).flatMap(ownerId ->
                        tags.findAllByOwnerIdAndResourceIdOrderByNameAsc(ownerId, resource.id()).collectList())
                        .defaultIfEmpty(java.util.List.of());
                return Mono.zip(
                    titles.findAllByResourceIdOrderByPrimaryDescLocaleAsc(resource.id()).collectList(),
                    resourceTagsMono,
                    (resourceTitles, resourceTags) -> {
                    var fields = new HashMap<String, Object>();
                    fields.put("type", resource.resourceType().name());
                    if (resource.primaryTitle() != null) fields.put("title", resource.primaryTitle());
                    if (resource.summary() != null) fields.put("summary", resource.summary());
                    fields.put("aliases", resourceTitles.stream().map(ResourceTitleEntity::title).toList());
                    fields.put("tags", new ArrayList<>(resourceTags.stream().map(ResourceTagEntity::name).toList()));
                    long version = resource.version() == null ? 0 : resource.version();
                    return new ResourceSearchProjection(resource.id(), version, fields);
                });
            });
    }

    @Override
    public Flux<ResourceSearchProjection> findAll() {
        return resources.findAll()
            .filter(resource -> resource.status() == ResourceStatus.ACTIVE.code())
            .concatMap(resource -> find(resource.id()));
    }

    private Mono<UUID> ownerId(UUID resourceId) {
        if (userResources == null) return Mono.empty();
        return userResources.findFirstByResourceIdAndRole(resourceId, "OWNER").map(UserResourceEntity::userId);
    }
}
