package run.ikaros.search;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.integration.api.DurableEvent;
import run.ikaros.resource.api.ResourceSearchProjection;
import run.ikaros.resource.api.ResourceSearchProjectionQuery;

class ResourceSearchProjectionConsumerTest {
    private final ResourceSearchProjectionQuery source = org.mockito.Mockito.mock(ResourceSearchProjectionQuery.class);
    private final SearchProjectionService projections = org.mockito.Mockito.mock(SearchProjectionService.class);
    private final ResourceSearchProjectionConsumer consumer =
        new ResourceSearchProjectionConsumer(source, projections);

    @Test
    void projectsCurrentResourceAfterChange() {
        UUID id = UUID.randomUUID();
        when(source.find(id)).thenReturn(Mono.just(new ResourceSearchProjection(id, 4,
            Map.of("type", "BOOK", "title", "A"))));
        when(projections.project(id, 4, Map.of("type", "BOOK", "title", "A"), "resource-v1", 0))
            .thenReturn(Mono.just(new SearchDocument(id, id, 4, "resource-v1", 0, Map.of(), Instant.now())));

        StepVerifier.create(consumer.consume(event("resource.resource.updated", id))).verifyComplete();

        verify(projections).project(id, 4, Map.of("type", "BOOK", "title", "A"), "resource-v1", 0);
    }

    @Test
    void deletesWhenResourceIsNoLongerProjectable() {
        UUID id = UUID.randomUUID();
        when(source.find(id)).thenReturn(Mono.empty());
        when(projections.delete(id)).thenReturn(Mono.empty());

        StepVerifier.create(consumer.consume(event("resource.resource.trashed", id))).verifyComplete();

        verify(projections).delete(id);
    }

    @Test
    void purgedResourceDeletesWithoutReadingSource() {
        UUID id = UUID.randomUUID();
        when(projections.delete(id)).thenReturn(Mono.empty());

        StepVerifier.create(consumer.consume(event("resource.resource.purged", id))).verifyComplete();

        verify(projections).delete(id);
        verifyNoInteractions(source);
    }

    @Test
    void ignoresUnrelatedEvent() {
        StepVerifier.create(consumer.consume(event("resource.user-state.changed", UUID.randomUUID())))
            .verifyComplete();
        verifyNoInteractions(source, projections);
    }

    private DurableEvent event(String type, UUID subjectId) {
        return new DurableEvent(UUID.randomUUID(), type, 1, "resource", "resource", subjectId,
            "{}", Instant.now(), null, null, null, null);
    }
}
