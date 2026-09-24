package run.ikaros.storage;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.ConflictException;
import run.ikaros.integration.api.DurableEventPublisher;
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
    private final DurableEventPublisher events = mock(DurableEventPublisher.class);
    private final StorageRestoreRequestService service = new StorageRestoreRequestService(attachments, resources,
        blobs, placements, requests, tasks, budget, events);

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

        verifyNoInteractions(attachments, resources, blobs, placements, tasks, budget, events);
    }

    @Test
    void repeatedRetryReturnsCommittedTaskWithoutDuplicateSideEffects() {
        UUID actorId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        StorageRestoreRequestEntity existing = new StorageRestoreRequestEntity(requestId, actorId,
            StorageRestoreScope.ATTACHMENT, UUID.randomUUID(), StorageRestoreRequestStatus.REQUESTED, 2, 1, 256,
            "restore-failed", "original-key", taskId, Instant.now(), Instant.now(), "ACCEPTED", null, 0L);
        when(requests.findById(requestId)).thenReturn(Mono.just(existing));

        StepVerifier.create(service.retry(actorId, requestId, "retry-key"))
            .assertNext(view -> {
                org.junit.jupiter.api.Assertions.assertEquals(requestId, view.id());
                org.junit.jupiter.api.Assertions.assertEquals(taskId, view.backgroundTaskId());
                org.junit.jupiter.api.Assertions.assertEquals(StorageRestoreRequestStatus.REQUESTED, view.status());
            })
            .verifyComplete();

        verifyNoInteractions(attachments, resources, blobs, placements, tasks, budget, events);
    }

    @Test
    void missingAttachmentIsRejectedBeforeBudgetCheckOrRequestSave() {
        UUID actorId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        when(requests.findByActorIdAndScopeAndScopeIdAndIdempotencyKey(actorId, StorageRestoreScope.ATTACHMENT,
            attachmentId, "restore-key")).thenReturn(Mono.empty());
        when(attachments.findById(attachmentId)).thenReturn(Mono.empty());

        StepVerifier.create(service.requestAttachment(actorId, new RequestAttachmentRestore(attachmentId, "STANDARD"),
            "restore-key"))
            .expectErrorSatisfies(error -> org.junit.jupiter.api.Assertions.assertInstanceOf(NotFoundException.class, error))
            .verify();

        verifyNoInteractions(resources, blobs, placements, tasks, budget, events);
    }

    @Test
    void attachmentSetIdempotencyKeyCannotBeReusedForDifferentContent() {
        UUID actorId = UUID.randomUUID();
        StorageRestoreRequestEntity existing = new StorageRestoreRequestEntity(UUID.randomUUID(), actorId,
            StorageRestoreScope.ATTACHMENT_SET, null, StorageRestoreRequestStatus.REQUESTED, 1, 0, 128,
            null, "restore-key", UUID.randomUUID(), Instant.now(), Instant.now(), "ACCEPTED",
            UUID.randomUUID().toString(), "original-request-fingerprint", 0L);
        when(requests.findByActorIdAndScopeAndIdempotencyKey(actorId, StorageRestoreScope.ATTACHMENT_SET,
            "restore-key")).thenReturn(Mono.just(existing));

        StepVerifier.create(service.requestAttachmentSet(actorId, List.of(UUID.randomUUID()), "STANDARD", null,
                "restore-key"))
            .expectError(ConflictException.class)
            .verify();

        verifyNoInteractions(attachments, resources, blobs, placements, tasks, budget, events);
    }
}
