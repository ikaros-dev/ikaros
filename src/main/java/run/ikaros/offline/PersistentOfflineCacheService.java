package run.ikaros.offline;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.ConflictException;
import run.ikaros.drive.DeviceRepository;
import run.ikaros.drive.DeviceTrustState;
@Service
public class PersistentOfflineCacheService implements OfflineCacheService {
    private final OfflineCacheEntryRepository entries;
    private final DeviceRepository devices;
    public PersistentOfflineCacheService(OfflineCacheEntryRepository entries, DeviceRepository devices){this.entries=entries;this.devices=devices;}
    @Override public Mono<CacheEntryView> put(UUID user,CreateCacheEntryRequest req){return devices.findById(req.deviceId()).filter(d->d.userId().equals(user)&&d.trustState()!=DeviceTrustState.REVOKED).switchIfEmpty(Mono.error(new ConflictException("Device 不存在或已撤销"))).flatMap(d->{Instant now=Instant.now();return entries.save(new OfflineCacheEntryEntity(null,user,req.deviceId(),req.resourceId(),req.attachmentId(),req.sizeBytes(),req.contentFingerprint(),CacheEntryState.ACTIVE,now,now,now,null));}).map(this::view);}
    @Override public Flux<CacheEntryView> list(UUID user,UUID device){return entries.findAllByUserIdAndDeviceIdOrderByLastAccessedAtDesc(user,device).take(100).filter(e->e.state()==CacheEntryState.ACTIVE).map(this::view);}
    @Override public Mono<CacheEntryView> touch(UUID user,UUID id){return owned(user,id).flatMap(e->entries.save(new OfflineCacheEntryEntity(e.id(),e.userId(),e.deviceId(),e.resourceId(),e.attachmentId(),e.sizeBytes(),e.contentFingerprint(),e.state(),Instant.now(),e.createdAt(),Instant.now(),e.version()))).map(this::view);}
    @Override public Mono<Void> evict(UUID user,UUID id){return owned(user,id).flatMap(e->entries.save(new OfflineCacheEntryEntity(e.id(),e.userId(),e.deviceId(),e.resourceId(),e.attachmentId(),e.sizeBytes(),e.contentFingerprint(),CacheEntryState.EVICTED,e.lastAccessedAt(),e.createdAt(),Instant.now(),e.version()))).then();}
    private Mono<OfflineCacheEntryEntity> owned(UUID user,UUID id){return entries.findById(id).filter(e->e.userId().equals(user)).switchIfEmpty(Mono.error(new NotFoundException("Cache Entry 不存在")));}
    private CacheEntryView view(OfflineCacheEntryEntity e){return new CacheEntryView(e.id(),e.userId(),e.deviceId(),e.resourceId(),e.attachmentId(),e.sizeBytes(),e.contentFingerprint(),e.state(),e.lastAccessedAt(),e.createdAt(),e.updatedAt());}
}
