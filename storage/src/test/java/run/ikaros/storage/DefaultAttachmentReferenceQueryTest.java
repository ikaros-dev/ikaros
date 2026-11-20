package run.ikaros.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.resource.api.ResourceOwnershipQuery;
import run.ikaros.storage.api.AttachmentKind;

class DefaultAttachmentReferenceQueryTest {
    private AttachmentRepository attachments;
    private ResourceOwnershipQuery resources;
    private DefaultAttachmentReferenceQuery query;

    @BeforeEach
    void setUp() {
        attachments = mock(AttachmentRepository.class);
        resources = mock(ResourceOwnershipQuery.class);
        query = new DefaultAttachmentReferenceQuery(attachments, resources);
    }

    @Test
    void requiresReadableAttachmentToBeActiveAndOwned() {
        UUID actorId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        Instant now = Instant.now();
        AttachmentReference attachment = new AttachmentReference(attachmentId, resourceId, UUID.randomUUID(),
            "cover.jpg", AttachmentKind.ORIGINAL, 1, actorId, null, null, now, now, 0L);
        when(attachments.findReadableReferenceById(attachmentId)).thenReturn(Mono.just(attachment));
        when(resources.requireReadable(actorId, resourceId)).thenReturn(Mono.empty());

        StepVerifier.create(query.requireReadable(actorId, attachmentId))
            .assertNext(reference -> {
                assertThat(reference.attachmentId()).isEqualTo(attachmentId);
                assertThat(reference.resourceId()).isEqualTo(resourceId);
            })
            .verifyComplete();
    }

    @Test
    void rejectsAttachmentOutsideResourceBoundary() {
        UUID actorId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        when(resources.requireReadable(actorId, resourceId)).thenReturn(Mono.empty());
        when(attachments.findActiveReferenceByIdAndResourceId(attachmentId, resourceId))
            .thenReturn(Mono.empty());

        StepVerifier.create(query.requireActiveForResource(actorId, resourceId, attachmentId))
            .expectErrorMessage("附件不存在或不属于指定 Resource")
            .verify();
    }
}
