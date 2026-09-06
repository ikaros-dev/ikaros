package run.ikaros.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * 验证 Placement 查询接口的参数透传。
 */
class StoragePlacementControllerTest {
    private StoragePlacementService service;
    private StoragePlacementController controller;

    @BeforeEach
    void setUp() {
        service = mock(StoragePlacementService.class);
        controller = new StoragePlacementController(service);
    }

    @Test
    void delegatesPlacementInspection() {
        UUID actorId = UUID.randomUUID();
        UUID blobId = UUID.randomUUID();
        StoragePlacementPlanView view = new StoragePlacementPlanView(blobId, StorageTier.COLD, 2, 1, false,
            List.of());
        when(service.inspect(blobId, StorageTier.COLD, 2)).thenReturn(Mono.just(view));

        StepVerifier.create(controller.inspect(actorId, blobId, StorageTier.COLD, 2))
            .assertNext(result -> assertThat(result).isEqualTo(view))
            .verifyComplete();

        verify(service).inspect(blobId, StorageTier.COLD, 2);
    }
}
