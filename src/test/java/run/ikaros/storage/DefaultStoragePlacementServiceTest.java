package run.ikaros.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * 验证 Blob 多级 Placement 规划的副本与层级判断。
 */
class DefaultStoragePlacementServiceTest {
    private BlobRepository blobRepository;
    private BlobPlacementRepository placementRepository;
    private DefaultStoragePlacementService service;

    @BeforeEach
    void setUp() {
        blobRepository = mock(BlobRepository.class);
        placementRepository = mock(BlobPlacementRepository.class);
        service = new DefaultStoragePlacementService(blobRepository, placementRepository);
    }

    @Test
    void reportsSatisfiedWhenActiveReplicasIncludePreferredTier() {
        UUID blobId = UUID.randomUUID();
        Instant now = Instant.now();
        BlobEntity blob = new BlobEntity(blobId, "f".repeat(64), 1L, "text/plain", BlobAvailability.AVAILABLE, now, 0L);
        BlobPlacementEntity active = new BlobPlacementEntity(UUID.randomUUID(), blobId, "hot-store", StorageTier.HOT,
            "objects/f", PlacementState.ACTIVE, now, now, 0L);
        BlobPlacementEntity cold = new BlobPlacementEntity(UUID.randomUUID(), blobId, "cold-store", StorageTier.COLD,
            "objects/f", PlacementState.UNAVAILABLE, null, now, 0L);
        when(blobRepository.findById(blobId)).thenReturn(Mono.just(blob));
        when(placementRepository.findAllByBlobIdOrderByCreatedAtAsc(blobId)).thenReturn(Flux.just(active, cold));

        StepVerifier.create(service.inspect(blobId, StorageTier.HOT, 1))
            .assertNext(plan -> {
                assertThat(plan.activeReplicaCount()).isEqualTo(1);
                assertThat(plan.satisfied()).isTrue();
                assertThat(plan.placements()).hasSize(2);
            })
            .verifyComplete();
    }

    @Test
    void reportsUnsatisfiedWhenPreferredTierIsUnavailable() {
        UUID blobId = UUID.randomUUID();
        Instant now = Instant.now();
        when(blobRepository.findById(blobId)).thenReturn(Mono.just(new BlobEntity(blobId, "g".repeat(64), 1L,
            "text/plain", BlobAvailability.AVAILABLE, now, 0L)));
        when(placementRepository.findAllByBlobIdOrderByCreatedAtAsc(blobId)).thenReturn(Flux.just(
            new BlobPlacementEntity(UUID.randomUUID(), blobId, "warm-store", StorageTier.WARM, "objects/g",
                PlacementState.ACTIVE, now, now, 0L)));

        StepVerifier.create(service.inspect(blobId, StorageTier.HOT, 1))
            .assertNext(plan -> assertThat(plan.satisfied()).isFalse())
            .verifyComplete();
    }
}
