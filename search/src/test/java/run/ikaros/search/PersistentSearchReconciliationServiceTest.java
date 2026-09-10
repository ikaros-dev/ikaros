package run.ikaros.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.resource.api.ResourceSearchProjection;

class PersistentSearchReconciliationServiceTest {
    private final SearchProjectionFailureRepository repository = org.mockito.Mockito.mock(SearchProjectionFailureRepository.class);
    private final run.ikaros.resource.api.ResourceSearchProjectionQuery source =
        org.mockito.Mockito.mock(run.ikaros.resource.api.ResourceSearchProjectionQuery.class);
    private final SearchProjectionService projections = org.mockito.Mockito.mock(SearchProjectionService.class);
    private final PersistentSearchReconciliationService service =
        new PersistentSearchReconciliationService(repository, source, projections);

    @Test
    void retriesCurrentProjectionAndResolvesFailure() {
        UUID failureId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        SearchProjectionFailureEntity failure = new SearchProjectionFailureEntity(failureId, sourceId, 1, 2,
            "temporary", Instant.now(), null);
        ResourceSearchProjection projection = new ResourceSearchProjection(sourceId, 3, Map.of("title", "new"));
        when(repository.findByIdAndResolvedAtIsNull(failureId)).thenReturn(Mono.just(failure));
        when(source.find(sourceId)).thenReturn(Mono.just(projection));
        when(projections.project(sourceId, 3, projection.fields(), "resource-v1", 0))
            .thenReturn(Mono.just(new SearchDocument(sourceId, sourceId, 3, "resource-v1", 0,
                projection.fields(), Instant.now())));
        when(repository.save(any(SearchProjectionFailureEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.retry(failureId))
            .assertNext(result -> assertThat(result.resolvedAt()).isNotNull())
            .verifyComplete();
    }
}
