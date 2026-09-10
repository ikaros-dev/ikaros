package run.ikaros.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.operations.api.AuditService;
import run.ikaros.operations.api.BackgroundTask;
import run.ikaros.operations.api.BackgroundTaskDispatcher;
import run.ikaros.storage.api.BlobGcCandidateView;
import run.ikaros.storage.api.StorageService;

class BlobGcTaskHandlerTest {
    private final StorageService storage = mock(StorageService.class);
    private final BlobGarbageCollector collector = mock(BlobGarbageCollector.class);
    private final DurableEventPublisher events = mock(DurableEventPublisher.class);
    private final AuditService audit = mock(AuditService.class);
    private final BlobGcTaskHandler handler = new BlobGcTaskHandler(mock(BackgroundTaskDispatcher.class), storage,
        collector, events, audit);

    @Test
    void successfulPurgeWritesAuditAndCompletionEvent() {
        UUID blobId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        BlobGcCandidateView candidate = new BlobGcCandidateView(blobId, "hash", 10, Instant.now(), Instant.now());
        BackgroundTask task = mock(BackgroundTask.class);
        when(task.id()).thenReturn(taskId);
        when(task.payload()).thenReturn(Map.of("limit", 1, "minimum_age_seconds", 10L,
            "requested_by", actorId.toString()));
        when(storage.findGarbageCollectionCandidates(1, java.time.Duration.ofSeconds(10))).thenReturn(Mono.just(List.of(candidate)));
        when(events.append(any())).thenReturn(Mono.empty());
        when(collector.purge(blobId)).thenReturn(Mono.just(2));
        when(audit.record(any(), any(), any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(task))
            .assertNext(result -> {
                assertEquals(1, result.get("purged_count"));
                assertEquals(List.of(blobId.toString()), result.get("blob_ids"));
            })
            .verifyComplete();

        verify(audit).record(actorId, "blob.gc.purge", "BLOB", blobId,
            "{\"purged_placement_count\":2,\"task_id\":\"" + taskId + "\"}");
        verify(events, org.mockito.Mockito.times(2)).append(any());
    }
}
