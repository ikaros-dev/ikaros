package run.ikaros.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.resource.api.MetadataSource;
import run.ikaros.resource.api.ResourceMetadataService;
import run.ikaros.resource.api.ResourceMetadataView;
import run.ikaros.resource.api.ResourceOwnershipQuery;

class DefaultMetadataSyncServiceTest {
    private final MetadataSyncSourceRepository sources = org.mockito.Mockito.mock(MetadataSyncSourceRepository.class);
    private final ResourceOwnershipQuery resources = org.mockito.Mockito.mock(ResourceOwnershipQuery.class);
    private final ResourceMetadataService metadata = org.mockito.Mockito.mock(ResourceMetadataService.class);
    private final MetadataCandidateService candidates = org.mockito.Mockito.mock(MetadataCandidateService.class);
    private final MetadataSyncStatusRepository statuses = org.mockito.Mockito.mock(MetadataSyncStatusRepository.class);
    private final DefaultMetadataSyncService service = new DefaultMetadataSyncService(sources, resources, metadata, candidates, statuses);

    @Test
    void createsCandidateWhenProviderValueChanges() {
        UUID owner = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        MetadataSyncSourceEntity source = source(sourceId, owner);
        MetadataCandidateView candidate = new MetadataCandidateView(UUID.randomUUID(), resourceId, "title", "new",
            MetadataSource.PROVIDER, "tmdb", 90, MetadataCandidateStatus.PENDING, Instant.now(), null);
        when(sources.findByIdAndOwnerId(sourceId, owner)).thenReturn(Mono.just(source));
        when(resources.requireOwned(owner, resourceId)).thenReturn(Mono.empty());
        when(metadata.list(owner, resourceId)).thenReturn(Flux.just(new ResourceMetadataView(UUID.randomUUID(),
            "title", "old", MetadataSource.USER, "user", true, true)));
        when(candidates.submit(any(UUID.class), any(UUID.class), any(SubmitMetadataCandidateRequest.class)))
            .thenReturn(Mono.just(candidate));
        when(statuses.save(any(MetadataSyncStatusEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.detect(owner, sourceId,
                new DetectMetadataUpdateRequest(resourceId, "title", "new", null, 90)))
            .assertNext(result -> {
                assertThat(result.status()).isEqualTo("CANDIDATE_CREATED");
                assertThat(result.candidate()).isEqualTo(candidate);
            })
            .verifyComplete();
    }

    @Test
    void reportsUnchangedWithoutCreatingCandidate() {
        UUID owner = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        when(sources.findByIdAndOwnerId(sourceId, owner)).thenReturn(Mono.just(source(sourceId, owner)));
        when(resources.requireOwned(owner, resourceId)).thenReturn(Mono.empty());
        when(metadata.list(owner, resourceId)).thenReturn(Flux.just(new ResourceMetadataView(UUID.randomUUID(),
            "title", "same", MetadataSource.PROVIDER, "tmdb", false, true)));
        when(statuses.save(any(MetadataSyncStatusEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.detect(owner, sourceId,
                new DetectMetadataUpdateRequest(resourceId, "title", "same", null, 90)))
            .assertNext(result -> {
                assertThat(result.status()).isEqualTo("UNCHANGED");
                assertThat(result.candidate()).isNull();
            })
            .verifyComplete();
        verify(candidates, never()).submit(any(), any(), any());
    }

    private MetadataSyncSourceEntity source(UUID id, UUID owner) {
        Instant now = Instant.now();
        return new MetadataSyncSourceEntity(id, owner, "tmdb", "TMDB", "secret://env/TMDB", "DAILY",
            MetadataSyncSourceStatus.ENABLED.name(), now, now, 0L);
    }
}
