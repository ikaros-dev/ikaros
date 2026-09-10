package run.ikaros.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.resource.api.ResourceOwnershipQuery;

class PersistentSearchQueryServiceTest {
    private final SearchDocumentRepository documents = Mockito.mock(SearchDocumentRepository.class);
    private final SearchProjectionService projections = Mockito.mock(SearchProjectionService.class);
    private final ResourceOwnershipQuery ownership = Mockito.mock(ResourceOwnershipQuery.class);
    private final PersistentSearchQueryService service =
        new PersistentSearchQueryService(documents, projections, ownership);
    private final UUID actor = UUID.randomUUID();

    @Test
    void returnsOnlyAuthorizedKeywordMatchesAndStableCursor() {
        UUID hidden = UUID.randomUUID();
        UUID visible = UUID.randomUUID();
        Instant hiddenAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant visibleAt = Instant.parse("2025-12-31T00:00:00Z");
        when(documents.search("%book%", "BOOK", "favorite", Instant.MAX,
            new UUID(Long.MAX_VALUE, Long.MAX_VALUE), 4))
            .thenReturn(Flux.just(entity(hidden, hiddenAt), entity(visible, visibleAt)));
        when(ownership.requireOwned(actor, hidden)).thenReturn(Mono.error(new IllegalStateException("denied")));
        when(ownership.requireOwned(actor, visible)).thenReturn(Mono.empty());
        when(projections.get(visible)).thenReturn(Mono.just(new SearchDocument(visible, visible, 3,
            "p0", 1, Map.of("title", "Book"), visibleAt)));

        StepVerifier.create(service.search(actor, new SearchQueryRequest("book", null, 1, " BOOK ", " favorite ")))
            .assertNext(page -> {
                assertThat(page.items()).extracting(SearchPage.SearchResult::resourceId).containsExactly(visible);
                assertThat(page.nextCursor()).isNotBlank();
            })
            .verifyComplete();
    }

    @Test
    void blankQueryReturnsEmptyPageWithoutReadingProjection() {
        StepVerifier.create(service.search(actor, new SearchQueryRequest(" ", null, 20, null, null)))
            .expectNext(new SearchPage(List.of(), null))
            .verifyComplete();
        verify(documents, never()).search(any(), any(), any(), any(), any(), any(Integer.class));
    }

    private static SearchDocumentEntity entity(UUID id, Instant at) {
        return new SearchDocumentEntity(id, id, 1, "p0", 1, "{\"title\":\"book\"}", at);
    }
}
