package run.ikaros.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.operations.api.AuditService;
import run.ikaros.operations.api.BackgroundTaskService;

class DefaultImportRunServiceTest {
    @Test
    void reusesRunForSameOwnerPlanAndIdempotencyKey() {
        ImportRunRepository runs = mock(ImportRunRepository.class);
        UUID ownerId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        ImportRunEntity existing = new ImportRunEntity(UUID.randomUUID(), planId, ownerId, ownerId,
            ImportRunStatus.PENDING.name(), null, 0, 0, 0, UUID.randomUUID(), Instant.now(), null,
            Instant.now(), "run-key", 0L);
        when(runs.findByOwnerIdAndIdempotencyKey(ownerId, "run-key")).thenReturn(Mono.just(existing));
        DefaultImportRunService service = new DefaultImportRunService(mock(ImportPlanRepository.class), runs,
            mock(BackgroundTaskService.class), mock(AuditService.class), mock(DurableEventPublisher.class),
            mock(ImportPlanItemRepository.class), mock(ImportRunItemRepository.class));

        ImportRunView result = service.start(ownerId, planId, new StartImportRequest(0L), "run-key").block();

        assertEquals(existing.id(), result.id());
    }

    @Test
    void rejectsSameKeyForAnotherPlan() {
        ImportRunRepository runs = mock(ImportRunRepository.class);
        UUID ownerId = UUID.randomUUID();
        ImportRunEntity existing = new ImportRunEntity(UUID.randomUUID(), UUID.randomUUID(), ownerId, ownerId,
            ImportRunStatus.PENDING.name(), null, 0, 0, 0, UUID.randomUUID(), Instant.now(), null,
            Instant.now(), "run-key", 0L);
        when(runs.findByOwnerIdAndIdempotencyKey(ownerId, "run-key")).thenReturn(Mono.just(existing));
        DefaultImportRunService service = new DefaultImportRunService(mock(ImportPlanRepository.class), runs,
            mock(BackgroundTaskService.class), mock(AuditService.class), mock(DurableEventPublisher.class),
            mock(ImportPlanItemRepository.class), mock(ImportRunItemRepository.class));

        RuntimeException error = assertThrows(RuntimeException.class,
            () -> service.start(ownerId, UUID.randomUUID(), new StartImportRequest(0L), "run-key").block());

        assertEquals("idempotency.key_reused", error.getCause() == null ? error.getMessage() : error.getCause().getMessage());
    }
}
