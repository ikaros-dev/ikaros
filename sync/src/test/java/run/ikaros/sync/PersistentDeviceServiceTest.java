package run.ikaros.sync;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.common.api.UuidV7Generator;

class PersistentDeviceServiceTest {
    @Test
    void registersNewDeviceAsActive() {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        DeviceRepository devices = mock(DeviceRepository.class);
        UuidV7Generator ids = mock(UuidV7Generator.class);
        when(devices.findByUserIdAndInstallationId(userId, "install-1")).thenReturn(Mono.empty());
        when(ids.next()).thenReturn(deviceId);
        when(devices.save(any(DeviceEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(new PersistentDeviceService(devices, ids).register(userId,
                new RegisterDeviceRequest("install-1", "我的手机", "ANDROID", "1.2.3")))
            .assertNext(view -> {
                org.assertj.core.api.Assertions.assertThat(view.id()).isEqualTo(deviceId);
                org.assertj.core.api.Assertions.assertThat(view.userId()).isEqualTo(userId);
                org.assertj.core.api.Assertions.assertThat(view.displayName()).isEqualTo("我的手机");
                org.assertj.core.api.Assertions.assertThat(view.trustState()).isEqualTo(DeviceTrustState.ACTIVE);
                org.assertj.core.api.Assertions.assertThat(view.registeredAt()).isNotNull();
            })
            .verifyComplete();
        verify(devices).save(any(DeviceEntity.class));
    }

    @Test
    void refreshesExistingInstallationWithoutCreatingAnotherDevice() {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        Instant registeredAt = Instant.parse("2026-09-01T00:00:00Z");
        DeviceEntity existing = new DeviceEntity(deviceId, userId, "install-1", "旧名称", "ANDROID", "1.0.0",
            DeviceTrustState.ACTIVE, registeredAt, registeredAt, null, 3L);
        DeviceRepository devices = mock(DeviceRepository.class);
        UuidV7Generator ids = mock(UuidV7Generator.class);
        when(devices.findByUserIdAndInstallationId(userId, "install-1")).thenReturn(Mono.just(existing));
        when(devices.save(any(DeviceEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(new PersistentDeviceService(devices, ids).register(userId,
                new RegisterDeviceRequest("install-1", "新名称", "ANDROID", "2.0.0")))
            .assertNext(view -> {
                org.assertj.core.api.Assertions.assertThat(view.id()).isEqualTo(deviceId);
                org.assertj.core.api.Assertions.assertThat(view.displayName()).isEqualTo("新名称");
                org.assertj.core.api.Assertions.assertThat(view.appVersion()).isEqualTo("2.0.0");
                org.assertj.core.api.Assertions.assertThat(view.registeredAt()).isEqualTo(registeredAt);
            })
            .verifyComplete();
        org.mockito.Mockito.verifyNoInteractions(ids);
    }
}
