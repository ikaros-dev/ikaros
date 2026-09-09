package run.ikaros.storage;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.media.api.MediaRestoreTargetQuery;
import run.ikaros.operations.api.BackgroundTaskService;
import run.ikaros.resource.api.ResourceOwnershipQuery;

class StorageRestoreRequestServiceTest {
    private final AttachmentRepository attachments = mock(AttachmentRepository.class);
    private final ResourceOwnershipQuery resources = mock(ResourceOwnershipQuery.class);
    private final BlobRepository blobs = mock(BlobRepository.class);
    private final BlobPlacementRepository placements = mock(BlobPlacementRepository.class);
    private final StorageRestoreRequestRepository requests = mock(StorageRestoreRequestRepository.class);
    private final BackgroundTaskService tasks = mock(BackgroundTaskService.class);
    private final StorageRestoreBudgetService budget = mock(StorageRestoreBudgetService.class);
    private final MediaRestoreTargetQuery mediaTargets = mock(MediaRestoreTargetQuery.class);
    private final DurableEventPublisher events = mock(DurableEventPublisher.class);
    private final StorageRestoreRequestService service = new StorageRestoreRequestService(attachments, resources,
        blobs, placements, requests, tasks, budget, mediaTargets, events);

    @Test
    void repeatedAttachmentRequestReturnsCommittedTaskWithoutDuplicateSideEffects() {
        UUID actorId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        StorageRestoreRequestEntity existing = new StorageRestoreRequestEntity(requestId, actorId,
            StorageRestoreScope.ATTACHMENT, attachmentId, StorageRestoreRequestStatus.REQUESTED, 1, 0, 128,
            null, "restore-key", taskId, Instant.now(), Instant.now(), "ACCEPTED", null, 0L);
        when(requests.findByActorIdAndScopeAndScopeIdAndIdempotencyKey(actorId, StorageRestoreScope.ATTACHMENT,
            attachmentId, "restore-key")).thenReturn(Mono.just(existing));

        StepVerifier.create(service.requestAttachment(actorId, new RequestAttachmentRestore(attachmentId, "STANDARD"),
            "restore-key"))
            .assertNext(view -> {
                org.junit.jupiter.api.Assertions.assertEquals(requestId, view.id());
                org.junit.jupiter.api.Assertions.assertEquals(taskId, view.backgroundTaskId());
                org.junit.jupiter.api.Assertions.assertEquals(StorageRestoreRequestStatus.REQUESTED, view.status());
            })
            .verifyComplete();

        verifyNoInteractions(attachments, resources, blobs, placements, tasks, budget, mediaTargets, events);
    }
}
