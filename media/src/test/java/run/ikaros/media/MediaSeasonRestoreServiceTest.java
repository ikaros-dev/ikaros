package run.ikaros.media;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.media.api.MediaRestoreTargetQuery;
import run.ikaros.storage.api.AttachmentReference;
import run.ikaros.storage.api.AttachmentReferenceQuery;
import run.ikaros.storage.api.StorageRestoreCapability;
import run.ikaros.storage.api.StorageRestoreSubmissionView;

class MediaSeasonRestoreServiceTest {
    @Test
    void expandsOwnedSeasonToAttachmentIdsBeforeCallingStorage() {
        UUID actorId = UUID.randomUUID();
        UUID seasonId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        MediaRestoreTargetQuery targets = mock(MediaRestoreTargetQuery.class);
        AttachmentReferenceQuery attachments = mock(AttachmentReferenceQuery.class);
        StorageRestoreCapability storage = mock(StorageRestoreCapability.class);
        StorageRestoreSubmissionView expected = new StorageRestoreSubmissionView(UUID.randomUUID(), "ATTACHMENT_SET",
            UUID.randomUUID(), "PENDING", 1, 128, 0, 0, "ACCEPTED", Instant.now());
        when(targets.requireOwnedSeason(actorId, seasonId)).thenReturn(Mono.empty());
        when(targets.findOwnedEpisodeResourceIds(actorId, seasonId)).thenReturn(Flux.just(resourceId));
        when(attachments.listActiveForResource(actorId, resourceId))
            .thenReturn(Flux.just(new AttachmentReference(attachmentId, resourceId)));
        when(storage.requestAttachmentSet(eq(actorId), eq(List.of(attachmentId)), any(), any(), eq("restore-key")))
            .thenReturn(Mono.just(expected));

        StepVerifier.create(new MediaSeasonRestoreService(targets, attachments, storage)
                .requestSeason(actorId, seasonId, new MediaSeasonRestoreOptions("STANDARD", null), "restore-key"))
            .expectNext(expected)
            .verifyComplete();

        verify(targets).requireOwnedSeason(actorId, seasonId);
        verify(storage).requestAttachmentSet(actorId, List.of(attachmentId), "STANDARD", null, "restore-key");
    }
}
