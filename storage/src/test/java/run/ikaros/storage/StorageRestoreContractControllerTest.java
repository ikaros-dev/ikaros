package run.ikaros.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class StorageRestoreContractControllerTest {
    private final StorageRestoreRequestService service = mock(StorageRestoreRequestService.class);
    private final StorageRestoreContractController controller = new StorageRestoreContractController(service);

    @Test
    void getExposesActiveProgressCounts() {
        UUID actorId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        when(service.get(actorId, requestId)).thenReturn(Mono.just(view(requestId,
            StorageRestoreRequestStatus.IN_PROGRESS, 4, 2, null)));

        StepVerifier.create(controller.get(actorId, requestId))
            .assertNext(result -> {
                assertEquals("ACTIVE", result.status());
                assertEquals(4, result.itemCount());
                assertEquals(2, result.readyItems());
                assertEquals(0, result.failedItems());
            })
            .verifyComplete();
    }

    @Test
    void getExposesFailedProgressCount() {
        UUID actorId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        when(service.get(actorId, requestId)).thenReturn(Mono.just(view(requestId,
            StorageRestoreRequestStatus.PARTIAL_FAILURE, 4, 2, "restore-failed")));

        StepVerifier.create(controller.get(actorId, requestId))
            .assertNext(result -> {
                assertEquals("PARTIAL", result.status());
                assertEquals(2, result.readyItems());
                assertEquals(2, result.failedItems());
            })
            .verifyComplete();
    }

    private StorageRestoreRequestView view(UUID requestId, StorageRestoreRequestStatus status,
        int totalItems, int completedItems, String errorSummary) {
        return new StorageRestoreRequestView(requestId, UUID.randomUUID(), StorageRestoreScope.ATTACHMENT,
            UUID.randomUUID(), status, totalItems, completedItems, 1024, errorSummary, UUID.randomUUID(),
            Instant.now(), Instant.now(), "ACCEPTED");
    }
}
