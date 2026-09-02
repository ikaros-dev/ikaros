package run.ikaros.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * 验证 Blob GC 候选扫描和决策接口的参数传递与响应语义。
 */
class BlobGarbageCollectionControllerTest {
    private StorageService storageService;
    private BlobGarbageCollectionController controller;

    @BeforeEach
    void setUp() {
        storageService = mock(StorageService.class);
        controller = new BlobGarbageCollectionController(storageService);
    }

    @Test
    void delegatesCandidateScanWithMinimumAge() {
        UUID actorId = UUID.randomUUID();
        when(storageService.findGarbageCollectionCandidates(20, Duration.ofHours(6)))
            .thenReturn(Mono.just(List.of()));

        StepVerifier.create(controller.findCandidates(actorId, 20, Duration.ofHours(6).toSeconds()))
            .assertNext(candidates -> assertThat(candidates).isEmpty())
            .verifyComplete();

        verify(storageService).findGarbageCollectionCandidates(20, Duration.ofHours(6));
    }

    @Test
    void recordsDecisionAndReturnsNoContent() {
        UUID actorId = UUID.randomUUID();
        UUID blobId = UUID.randomUUID();
        when(storageService.recordGarbageCollectionDecision(actorId, blobId, true)).thenReturn(Mono.empty());

        StepVerifier.create(controller.recordDecision(actorId, blobId, new GarbageCollectionDecisionRequest(true)))
            .assertNext(response -> assertThat(response).isEqualTo(ResponseEntity.noContent().build()))
            .verifyComplete();

        verify(storageService).recordGarbageCollectionDecision(actorId, blobId, true);
    }
}
