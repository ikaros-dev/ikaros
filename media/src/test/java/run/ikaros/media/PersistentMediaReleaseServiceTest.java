package run.ikaros.media;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.resource.api.ResourceClassification;
import run.ikaros.resource.api.ResourceLifecycle;
import run.ikaros.resource.api.ResourceService;
import run.ikaros.resource.api.ResourceType;
import run.ikaros.resource.api.ResourceView;
import run.ikaros.storage.api.AttachmentReference;
import run.ikaros.storage.api.AttachmentReferenceQuery;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PersistentMediaReleaseServiceTest {
    @Test
    void associatesOnlyActiveAttachmentOwnedByResource() {
        UUID owner = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        ResourceService resources = org.mockito.Mockito.mock(ResourceService.class);
        AttachmentReferenceQuery attachments = org.mockito.Mockito.mock(AttachmentReferenceQuery.class);
        MediaReleaseRepository releases = org.mockito.Mockito.mock(MediaReleaseRepository.class);
        when(resources.get(owner, resourceId)).thenReturn(Mono.just(new ResourceView(resourceId, ResourceType.VIDEO, "视频", null,
            ResourceClassification.PRIVATE, ResourceLifecycle.ACTIVE, List.of(), List.of(), Instant.now(), Instant.now(), 0L)));
        when(attachments.requireActiveForResource(owner, resourceId, attachmentId)).thenReturn(Mono.just(new AttachmentReference(attachmentId, resourceId)));
        when(releases.save(any(MediaReleaseEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(new PersistentMediaReleaseService(resources, attachments, releases).add(owner, resourceId,
                new CreateMediaReleaseRequest(attachmentId, "WEB", "1080P", "sha256:test")))
            .expectNextMatches(view -> view.playableResourceId().equals(resourceId)
                && view.attachmentId().equals(attachmentId) && view.state() == MediaReleaseState.AVAILABLE)
            .verifyComplete();
        verify(attachments).requireActiveForResource(owner, resourceId, attachmentId);
        verify(releases).save(any(MediaReleaseEntity.class));
    }

    @Test
    void doesNotSaveWhenAttachmentIsNotActiveForResource() {
        UUID owner = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        ResourceService resources = org.mockito.Mockito.mock(ResourceService.class);
        AttachmentReferenceQuery attachments = org.mockito.Mockito.mock(AttachmentReferenceQuery.class);
        MediaReleaseRepository releases = org.mockito.Mockito.mock(MediaReleaseRepository.class);
        when(resources.get(owner, resourceId)).thenReturn(Mono.just(new ResourceView(resourceId, ResourceType.VIDEO, "视频", null,
            ResourceClassification.PRIVATE, ResourceLifecycle.ACTIVE, List.of(), List.of(), Instant.now(), Instant.now(), 0L)));
        when(attachments.requireActiveForResource(owner, resourceId, attachmentId)).thenReturn(Mono.error(new IllegalStateException("attachment unavailable")));

        StepVerifier.create(new PersistentMediaReleaseService(resources, attachments, releases).add(owner, resourceId,
                new CreateMediaReleaseRequest(attachmentId, null, null, null)))
            .expectErrorMessage("attachment unavailable")
            .verify();
        verify(releases, never()).save(any(MediaReleaseEntity.class));
    }
}
