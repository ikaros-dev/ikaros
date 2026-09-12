package run.ikaros.offline;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.sync.api.DeviceTrustQuery;

@Service
public class PersistentOfflineCacheQuotaService implements OfflineCacheQuotaService {
    private static final long DEFAULT_QUOTA_BYTES = 1_073_741_824L;
    private final OfflineCacheQuotaRepository quotas;
    private final OfflineCacheEntryRepository entries;
    private final DeviceTrustQuery devices;

    public PersistentOfflineCacheQuotaService(OfflineCacheQuotaRepository quotas,
        OfflineCacheEntryRepository entries, DeviceTrustQuery devices) {
        this.quotas = quotas; this.entries = entries; this.devices = devices;
    }

    @Override public Mono<OfflineCacheQuotaView> get(UUID userId, UUID deviceId) {
        return usable(userId, deviceId).then(quotas.findByUserIdAndDeviceId(userId, deviceId)
            .defaultIfEmpty(new OfflineCacheQuotaEntity(null, userId, deviceId, DEFAULT_QUOTA_BYTES, null, null, null)))
            .flatMap(this::view);
    }

    @Override public Mono<OfflineCacheQuotaView> set(UUID userId, SetOfflineCacheQuotaRequest request) {
        return usable(userId, request.deviceId()).then(quotas.findByUserIdAndDeviceId(userId, request.deviceId())
            .flatMap(old -> quotas.save(new OfflineCacheQuotaEntity(old.id(), userId, old.deviceId(), request.quotaBytes(), old.createdAt(), Instant.now(), old.version())))
            .switchIfEmpty(Mono.defer(() -> quotas.save(new OfflineCacheQuotaEntity(null, userId, request.deviceId(), request.quotaBytes(), Instant.now(), Instant.now(), null)))))
            .flatMap(this::view);
    }

    private Mono<Void> usable(UUID userId, UUID deviceId) {
        return devices.isUsable(userId, deviceId).filter(Boolean.TRUE::equals)
            .switchIfEmpty(Mono.error(new ConflictException("Device 不存在或已撤销"))).then();
    }

    private Mono<OfflineCacheQuotaView> view(OfflineCacheQuotaEntity quota) {
        return entries.findAllByUserIdAndDeviceIdOrderByLastAccessedAtDesc(quota.userId(), quota.deviceId())
            .filter(entry -> entry.state() == CacheEntryState.ACTIVE).map(OfflineCacheEntryEntity::sizeBytes).reduce(0L, Long::sum)
            .map(used -> new OfflineCacheQuotaView(quota.deviceId(), quota.quotaBytes(), used,
                Math.max(0L, quota.quotaBytes() - used), quota.updatedAt()));
    }
}
