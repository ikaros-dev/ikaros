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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.sync.api.DeviceTrustQuery;

@ExtendWith(MockitoExtension.class)
class PersistentOfflineCacheEvictionTest {
    @Mock OfflineCacheEntryRepository entries;
    @Mock DownloadIntentRepository downloads;
    @Mock DeviceTrustQuery devices;

    @Test
    void evictsActiveCacheButProtectsExplicitDownload() {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        UUID protectedResource = UUID.randomUUID();
        UUID protectedAttachment = UUID.randomUUID();
        OfflineCacheEntryEntity protectedEntry = entry(userId, deviceId, protectedResource, protectedAttachment, 100L);
        OfflineCacheEntryEntity eligibleEntry = entry(userId, deviceId, UUID.randomUUID(), UUID.randomUUID(), 250L);
        when(devices.isUsable(userId, deviceId)).thenReturn(Mono.just(true));
        when(downloads.findAllByUserIdAndDeviceIdOrderByCreatedAtDesc(userId, deviceId))
            .thenReturn(Flux.just(download(userId, deviceId, protectedResource, protectedAttachment,
                OfflineCopyKind.DOWNLOAD, DownloadState.COMPLETED)));
        when(entries.findAllByUserIdAndDeviceIdOrderByLastAccessedAtDesc(userId, deviceId))
            .thenReturn(Flux.just(protectedEntry, eligibleEntry));
        when(entries.save(any(OfflineCacheEntryEntity.class))).thenAnswer(invocation ->
            Mono.just(invocation.getArgument(0)));

        StepVerifier.create(new PersistentOfflineCacheService(entries, downloads, devices)
                .evictEligible(userId, deviceId))
            .assertNext(result -> {
                assertThat(result.deviceId()).isEqualTo(deviceId);
                assertThat(result.evictedCount()).isEqualTo(1);
                assertThat(result.evictedBytes()).isEqualTo(250L);
                assertThat(result.protectedDownloadCount()).isEqualTo(1);
            })
            .verifyComplete();
        verify(entries).save(org.mockito.ArgumentMatchers.argThat(saved ->
            saved.id().equals(eligibleEntry.id()) && saved.state() == CacheEntryState.EVICTED));
        verify(entries, never()).save(org.mockito.ArgumentMatchers.argThat(saved ->
            saved.id().equals(protectedEntry.id())));
    }

    @Test
    void removedDownloadDoesNotProtectCacheEntry() {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        OfflineCacheEntryEntity cacheEntry = entry(userId, deviceId, UUID.randomUUID(), UUID.randomUUID(), 64L);
        when(devices.isUsable(userId, deviceId)).thenReturn(Mono.just(true));
        when(downloads.findAllByUserIdAndDeviceIdOrderByCreatedAtDesc(userId, deviceId))
            .thenReturn(Flux.just(download(userId, deviceId, cacheEntry.resourceId(), cacheEntry.attachmentId(),
                OfflineCopyKind.DOWNLOAD, DownloadState.REMOVED)));
        when(entries.findAllByUserIdAndDeviceIdOrderByLastAccessedAtDesc(userId, deviceId))
            .thenReturn(Flux.just(cacheEntry));
        when(entries.save(any(OfflineCacheEntryEntity.class))).thenAnswer(invocation ->
            Mono.just(invocation.getArgument(0)));

        StepVerifier.create(new PersistentOfflineCacheService(entries, downloads, devices)
                .evictEligible(userId, deviceId))
            .assertNext(result -> {
                assertThat(result.evictedCount()).isEqualTo(1);
                assertThat(result.evictedBytes()).isEqualTo(64L);
                assertThat(result.protectedDownloadCount()).isZero();
            })
            .verifyComplete();
    }

    @Test
    void rejectsRevokedDeviceBeforeReadingCache() {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        when(devices.isUsable(userId, deviceId)).thenReturn(Mono.just(false));

        StepVerifier.create(new PersistentOfflineCacheService(entries, downloads, devices)
                .evictEligible(userId, deviceId))
            .expectErrorMessage("Device 不存在或已撤销")
            .verify();
        verify(entries, never()).findAllByUserIdAndDeviceIdOrderByLastAccessedAtDesc(userId, deviceId);
        verify(downloads, never()).findAllByUserIdAndDeviceIdOrderByCreatedAtDesc(userId, deviceId);
    }

    private static OfflineCacheEntryEntity entry(UUID userId, UUID deviceId, UUID resourceId,
            UUID attachmentId, long sizeBytes) {
        Instant now = Instant.now();
        return new OfflineCacheEntryEntity(UUID.randomUUID(), userId, deviceId, resourceId, attachmentId,
            sizeBytes, "sha256:test", CacheEntryState.ACTIVE, now, now, now, 0L);
    }

    private static DownloadIntentEntity download(UUID userId, UUID deviceId, UUID resourceId,
            UUID attachmentId, OfflineCopyKind kind, DownloadState state) {
        Instant now = Instant.now();
        return new DownloadIntentEntity(UUID.randomUUID(), userId, deviceId, resourceId, attachmentId,
            kind, state, null, 1, now, now, 0L);
    }
}
