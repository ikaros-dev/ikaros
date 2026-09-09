package run.ikaros.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.common.NotFoundException;
import run.ikaros.resource.api.ResourceOwnershipQuery;

class PersistentDeliveryGrantServiceTest {
    @Test
    void authorizesOwnerAndRejectsOtherOwnerExpiredAndUnknownGrants() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        AttachmentRepository attachments = mock(AttachmentRepository.class);
        ResourceOwnershipQuery resources = mock(ResourceOwnershipQuery.class);
        MediaDeliveryGrantRepository grants = mock(MediaDeliveryGrantRepository.class);
        when(grants.findByTokenHash(any())).thenReturn(Mono.just(grant(attachmentId, owner, Instant.now().plusSeconds(60))))
            .thenReturn(Mono.just(grant(attachmentId, owner, Instant.now().minusSeconds(1))))
            .thenReturn(Mono.empty());
        var service = new PersistentDeliveryGrantService(attachments, resources, grants);

        StepVerifier.create(service.authorize(owner, attachmentId, "token", null))
            .assertNext(result -> assertThat(result).isEqualTo(owner)).verifyComplete();
        StepVerifier.create(service.authorize(other, attachmentId, "token", null))
            .expectError(NotFoundException.class).verify();
        StepVerifier.create(service.authorize(owner, attachmentId, "token", null))
            .expectError(NotFoundException.class).verify();
        StepVerifier.create(service.authorize(owner, attachmentId, "token", null))
            .expectError(NotFoundException.class).verify();
    }

    private MediaDeliveryGrantEntity grant(UUID attachmentId, UUID ownerId, Instant expiresAt) {
        Instant now = Instant.now();
        return new MediaDeliveryGrantEntity(UUID.randomUUID(), attachmentId, ownerId, "hash", "GET", null, null,
            expiresAt, run.ikaros.storage.api.DeliveryGrantRevocationLevel.IMMEDIATE, null, now, 0L);
    }
}
