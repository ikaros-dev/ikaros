package run.ikaros.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
class PersistentOfflineCacheQuotaServiceTest {
    @Mock OfflineCacheQuotaRepository quotas;
    @Mock OfflineCacheEntryRepository entries;
    @Mock DeviceTrustQuery devices;

    @Test
    void returnsDefaultQuotaAndCountsActiveEntries() {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        when(devices.isUsable(userId, deviceId)).thenReturn(Mono.just(true));
        when(quotas.findByUserIdAndDeviceId(userId, deviceId)).thenReturn(Mono.empty());
        when(entries.findAllByUserIdAndDeviceIdOrderByLastAccessedAtDesc(userId, deviceId))
            .thenReturn(Flux.just(entry(userId, deviceId, 300, CacheEntryState.ACTIVE),
                entry(userId, deviceId, 200, CacheEntryState.EVICTED)));

        StepVerifier.create(new PersistentOfflineCacheQuotaService(quotas, entries, devices)
                .get(userId, deviceId))
            .assertNext(view -> {
                assertThat(view.deviceId()).isEqualTo(deviceId);
                assertThat(view.quotaBytes()).isEqualTo(1_073_741_824L);
                assertThat(view.usedBytes()).isEqualTo(300);
                assertThat(view.availableBytes()).isEqualTo(1_073_741_524L);
                assertThat(view.updatedAt()).isNull();
            })
            .verifyComplete();
    }

    @Test
    void savesQuotaForUsableDeviceAndRejectsRevokedDevice() {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        SetOfflineCacheQuotaRequest request = new SetOfflineCacheQuotaRequest(deviceId, 4096);
        when(devices.isUsable(userId, deviceId)).thenReturn(Mono.just(true), Mono.just(false));
        when(quotas.findByUserIdAndDeviceId(userId, deviceId)).thenReturn(Mono.empty());
        when(quotas.save(any(OfflineCacheQuotaEntity.class))).thenAnswer(invocation ->
            Mono.just(invocation.getArgument(0)));
        when(entries.findAllByUserIdAndDeviceIdOrderByLastAccessedAtDesc(userId, deviceId))
            .thenReturn(Flux.empty());

        StepVerifier.create(new PersistentOfflineCacheQuotaService(quotas, entries, devices)
                .set(userId, request))
            .assertNext(view -> {
                assertThat(view.quotaBytes()).isEqualTo(4096);
                assertThat(view.usedBytes()).isZero();
                assertThat(view.availableBytes()).isEqualTo(4096);
                assertThat(view.updatedAt()).isNotNull();
            })
            .verifyComplete();
        verify(quotas).save(org.mockito.ArgumentMatchers.argThat(saved ->
            saved.userId().equals(userId) && saved.deviceId().equals(deviceId)
                && saved.quotaBytes() == 4096));

        StepVerifier.create(new PersistentOfflineCacheQuotaService(quotas, entries, devices)
                .set(userId, request))
            .expectErrorMessage("Device 不存在或已撤销")
            .verify();
        verify(quotas, org.mockito.Mockito.times(2)).findByUserIdAndDeviceId(userId, deviceId);
    }

    private static OfflineCacheEntryEntity entry(UUID userId, UUID deviceId, long size,
        CacheEntryState state) {
        Instant now = Instant.now();
        return new OfflineCacheEntryEntity(UUID.randomUUID(), userId, deviceId, UUID.randomUUID(),
            UUID.randomUUID(), size, "sha256", state, now, now, now, 0L);
    }
}
