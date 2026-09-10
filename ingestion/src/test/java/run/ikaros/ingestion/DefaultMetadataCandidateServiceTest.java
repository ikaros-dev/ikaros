package run.ikaros.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.resource.api.ResourceMetadataService;
import run.ikaros.resource.api.ResourceMetadataView;
import run.ikaros.resource.api.ResourceOwnershipQuery;

@ExtendWith(MockitoExtension.class)
class DefaultMetadataCandidateServiceTest {
    @Mock private ResourceOwnershipQuery resources;
    @Mock private MetadataCandidateRepository candidates;
    @Mock private ResourceMetadataService metadata;

    private DefaultMetadataCandidateService service;
    private UUID ownerId;
    private UUID resourceId;
    private UUID candidateId;

    @BeforeEach
    void setUp() {
        service = new DefaultMetadataCandidateService(resources, candidates, metadata);
        ownerId = UUID.randomUUID();
        resourceId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        when(resources.requireOwned(ownerId, resourceId)).thenReturn(Mono.empty());
    }

    @Test
    void rejectPersistsResolvedStatus() {
        MetadataCandidateEntity candidate = candidate(MetadataCandidateStatus.PENDING.name());
        when(candidates.findById(candidateId)).thenReturn(Mono.just(candidate));
        when(candidates.save(any(MetadataCandidateEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.resolve(ownerId, candidateId,
                new ResolveMetadataCandidateRequest(MetadataCandidateResolution.REJECT)))
            .assertNext(view -> assertEquals(MetadataCandidateStatus.REJECTED, view.status()))
            .verifyComplete();
    }

    @Test
    void applyingLockedFieldDoesNotResolveCandidate() {
        MetadataCandidateEntity candidate = candidate(MetadataCandidateStatus.PENDING.name());
        when(candidates.findById(candidateId)).thenReturn(Mono.just(candidate));
        when(metadata.applyAutomatic(any(), any(), any(), any())).thenReturn(Mono.just(
            new ResourceMetadataView(UUID.randomUUID(), "summary", "manual", null, null, true, false)));

        StepVerifier.create(service.resolve(ownerId, candidateId,
                new ResolveMetadataCandidateRequest(MetadataCandidateResolution.APPLY)))
            .expectErrorMatches(error -> error instanceof IllegalStateException
                && error.getMessage().contains("人工锁定"))
            .verify();
    }

    private MetadataCandidateEntity candidate(String status) {
        return new MetadataCandidateEntity(candidateId, resourceId, "summary", "external", "PROVIDER",
            "provider://item", 90, status, Instant.now(), null, 0L);
    }
}
