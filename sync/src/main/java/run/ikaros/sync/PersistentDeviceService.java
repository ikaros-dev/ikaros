package run.ikaros.sync;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.api.UuidV7Generator;
import run.ikaros.sync.api.DeviceTrustQuery;

@Service
public class PersistentDeviceService implements DeviceService, DeviceTrustQuery {
    private final DeviceRepository devices;
    private final UuidV7Generator ids;

    public PersistentDeviceService(DeviceRepository devices, UuidV7Generator ids) {
        this.devices = devices;
        this.ids = ids;
    }

    @Override
    public Mono<DeviceView> register(UUID userId, RegisterDeviceRequest request) {
        return devices.findByUserIdAndInstallationId(userId, request.installationId())
            .flatMap(existing -> devices.save(new DeviceEntity(existing.id(), userId,
                existing.installationId(), request.displayName().trim(), request.platform(),
                request.appVersion(), existing.trustState(), existing.registeredAt(), Instant.now(),
                existing.revokedAt(), existing.version())))
            .switchIfEmpty(Mono.defer(() -> {
                Instant now = Instant.now();
                return devices.save(new DeviceEntity(ids.next(), userId, request.installationId(),
                    request.displayName().trim(), request.platform(), request.appVersion(),
                    DeviceTrustState.ACTIVE, now, now, null, null));
            }))
            .map(this::view);
    }

    @Override
    public Flux<DeviceView> list(UUID userId) {
        return devices.findAllByUserIdOrderByRegisteredAtAsc(userId).take(100).map(this::view);
    }

    @Override
    public Mono<DeviceView> revoke(UUID userId, UUID deviceId) {
        return devices.findById(deviceId)
            .filter(device -> device.userId().equals(userId))
            .switchIfEmpty(Mono.error(new NotFoundException("Device 不存在")))
            .flatMap(device -> devices.save(new DeviceEntity(device.id(), device.userId(),
                device.installationId(), device.displayName(), device.platform(), device.appVersion(),
                DeviceTrustState.REVOKED, device.registeredAt(), device.lastSeenAt(), Instant.now(),
                device.version())))
            .map(this::view);
    }

    @Override
    public Mono<Boolean> isUsable(UUID userId, UUID deviceId) {
        return devices.findById(deviceId)
            .map(device -> device.userId().equals(userId) && device.trustState() != DeviceTrustState.REVOKED)
            .defaultIfEmpty(false);
    }

    private DeviceView view(DeviceEntity device) {
        return new DeviceView(device.id(), device.userId(), device.installationId(), device.displayName(),
            device.platform(), device.appVersion(), device.trustState(), device.registeredAt(),
            device.lastSeenAt(), device.revokedAt());
    }
}
