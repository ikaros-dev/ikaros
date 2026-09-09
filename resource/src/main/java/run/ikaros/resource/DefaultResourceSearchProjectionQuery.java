package run.ikaros.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.resource.api.ResourceLifecycle;
import run.ikaros.resource.api.ResourceSearchProjection;
import run.ikaros.resource.api.ResourceSearchProjectionQuery;

@Service
public class DefaultResourceSearchProjectionQuery implements ResourceSearchProjectionQuery {
    private final ResourceRepository resources;
    private final ResourceTitleRepository titles;
    private final ResourceTagRepository tags;

    public DefaultResourceSearchProjectionQuery(ResourceRepository resources, ResourceTitleRepository titles,
                                                ResourceTagRepository tags) {
        this.resources = resources;
        this.titles = titles;
        this.tags = tags;
    }

    @Override
    public Mono<ResourceSearchProjection> find(UUID resourceId) {
        return resources.findById(resourceId)
            .filter(resource -> resource.lifecycle() == ResourceLifecycle.ACTIVE)
            .flatMap(resource -> Mono.zip(
                titles.findAllByResourceIdOrderByPrimaryDescLocaleAsc(resource.id()).collectList(),
                tags.findAllByOwnerIdAndResourceIdOrderByNameAsc(resource.ownerId(), resource.id()).collectList(),
                (resourceTitles, resourceTags) -> {
                    var fields = new HashMap<String, Object>();
                    fields.put("type", resource.resourceType().name());
                    if (resource.primaryTitle() != null) fields.put("title", resource.primaryTitle());
                    if (resource.summary() != null) fields.put("summary", resource.summary());
                    fields.put("aliases", resourceTitles.stream().map(ResourceTitleEntity::title).toList());
                    fields.put("tags", new ArrayList<>(resourceTags.stream().map(ResourceTagEntity::name).toList()));
                    long version = resource.version() == null ? 0 : resource.version();
                    return new ResourceSearchProjection(resource.id(), version, fields);
                }));
    }
}
