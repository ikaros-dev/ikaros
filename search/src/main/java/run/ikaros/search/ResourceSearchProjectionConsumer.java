package run.ikaros.search;

import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.integration.api.DurableEvent;
import run.ikaros.integration.api.DurableEventConsumer;
import run.ikaros.resource.api.ResourceSearchProjectionQuery;

/** Rebuilds the search projection after durable Resource changes. */
@Component
public final class ResourceSearchProjectionConsumer implements DurableEventConsumer {
    private static final String CONSUMER_ID = "search.resource-projection.v1";
    private static final String PROJECTOR_VERSION = "resource-v1";
    private static final Set<String> RELEVANT_EVENTS = Set.of(
        "resource.resource.created", "resource.resource.updated", "resource.resource.trashed",
        "resource.resource.archived", "resource.resource.restored", "resource.resource.purged",
        "resource.tag.added", "resource.tag.removed");

    private final ResourceSearchProjectionQuery source;
    private final SearchProjectionService projections;

    public ResourceSearchProjectionConsumer(ResourceSearchProjectionQuery source,
                                            SearchProjectionService projections) {
        this.source = source;
        this.projections = projections;
    }

    @Override
    public String consumerId() {
        return CONSUMER_ID;
    }

    @Override
    public Mono<Void> consume(DurableEvent event) {
        if (event == null || !RELEVANT_EVENTS.contains(event.eventType()) || event.subjectId() == null) {
            return Mono.empty();
        }
        UUID resourceId = event.subjectId();
        if (event.eventType().equals("resource.resource.purged")) {
            return projections.delete(resourceId);
        }
        return source.find(resourceId)
            .flatMap(projection -> projections.project(resourceId, projection.sourceVersion(),
                projection.fields(), PROJECTOR_VERSION, 0).thenReturn(Boolean.TRUE))
            .switchIfEmpty(Mono.defer(() -> projections.delete(resourceId).thenReturn(Boolean.FALSE)))
            .then();
    }
}
