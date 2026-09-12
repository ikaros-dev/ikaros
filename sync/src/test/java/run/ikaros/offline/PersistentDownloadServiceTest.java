package run.ikaros.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.sync.api.DeviceTrustQuery;

@ExtendWith(MockitoExtension.class)
class PersistentDownloadServiceTest {
    @Mock DownloadIntentRepository repository;
    @Mock DeviceTrustQuery devices;

    @Test
    void createsQueuedDownloadForUsableDevice() {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        UUID downloadId = UUID.randomUUID();
        when(devices.isUsable(userId, deviceId)).thenReturn(Mono.just(true));
        when(repository.save(any(DownloadIntentEntity.class))).thenAnswer(invocation -> {
            DownloadIntentEntity request = invocation.getArgument(0);
            return Mono.just(new DownloadIntentEntity(downloadId, request.userId(), request.deviceId(),
                request.resourceId(), request.attachmentId(), request.kind(), request.state(),
                request.failureReason(), request.manifestVersion(), request.createdAt(),
                request.updatedAt(), 0L));
        });

        StepVerifier.create(new PersistentDownloadService(repository, devices).create(userId,
                new CreateDownloadRequest(deviceId, resourceId, attachmentId, OfflineCopyKind.DOWNLOAD)))
            .assertNext(view -> {
                assertThat(view.id()).isEqualTo(downloadId);
                assertThat(view.userId()).isEqualTo(userId);
                assertThat(view.deviceId()).isEqualTo(deviceId);
                assertThat(view.resourceId()).isEqualTo(resourceId);
                assertThat(view.attachmentId()).isEqualTo(attachmentId);
                assertThat(view.kind()).isEqualTo(OfflineCopyKind.DOWNLOAD);
                assertThat(view.state()).isEqualTo(DownloadState.QUEUED);
                assertThat(view.manifestVersion()).isEqualTo(1);
                assertThat(view.createdAt()).isBeforeOrEqualTo(Instant.now());
            })
            .verifyComplete();
    }

    @Test
    void rejectsDownloadForRevokedDevice() {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        when(devices.isUsable(userId, deviceId)).thenReturn(Mono.just(false));

        StepVerifier.create(new PersistentDownloadService(repository, devices).create(userId,
                new CreateDownloadRequest(deviceId, UUID.randomUUID(), null, null)))
            .expectErrorMessage("Device 不存在或已撤销")
            .verify();
    }

    @Test
    void removesOwnedDownloadWithoutDeletingItsResource() {
        UUID userId = UUID.randomUUID();
        UUID intentId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        DownloadIntentEntity existing = new DownloadIntentEntity(intentId, userId, deviceId, resourceId,
            null, OfflineCopyKind.DOWNLOAD, DownloadState.COMPLETED, null, 2,
            Instant.now().minusSeconds(10), Instant.now(), 1L);
        when(repository.findById(intentId)).thenReturn(Mono.just(existing));
        when(repository.save(any(DownloadIntentEntity.class))).thenAnswer(invocation ->
            Mono.just(invocation.getArgument(0)));

        StepVerifier.create(new PersistentDownloadService(repository, devices).remove(userId, intentId))
            .assertNext(view -> {
                assertThat(view.id()).isEqualTo(intentId);
                assertThat(view.resourceId()).isEqualTo(resourceId);
                assertThat(view.state()).isEqualTo(DownloadState.REMOVED);
            })
            .verifyComplete();
        verify(repository).save(org.mockito.ArgumentMatchers.argThat(saved ->
            saved.id().equals(intentId)
                && saved.resourceId().equals(resourceId)
                && saved.state() == DownloadState.REMOVED));
    }

    @Test
    void cannotRemoveAnotherUsersDownload() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID intentId = UUID.randomUUID();
        DownloadIntentEntity existing = new DownloadIntentEntity(intentId, ownerId, UUID.randomUUID(),
            UUID.randomUUID(), null, OfflineCopyKind.DOWNLOAD, DownloadState.QUEUED, null, 1,
            Instant.now(), Instant.now(), 0L);
        when(repository.findById(intentId)).thenReturn(Mono.just(existing));

        StepVerifier.create(new PersistentDownloadService(repository, devices).remove(otherUserId, intentId))
            .expectErrorMessage("Download 不存在")
            .verify();
        verify(repository, never()).save(any(DownloadIntentEntity.class));
    }
}
