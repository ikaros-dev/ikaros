package run.ikaros.offline;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.ConflictException;
import run.ikaros.sync.api.DeviceTrustQuery;
import java.util.Objects;
import java.util.List;
@Service
public class PersistentOfflineCacheService implements OfflineCacheService {
    private final OfflineCacheEntryRepository entries;
    private final DownloadIntentRepository downloads;
    private final DeviceTrustQuery devices;
    public PersistentOfflineCacheService(OfflineCacheEntryRepository entries, DownloadIntentRepository downloads, DeviceTrustQuery devices){this.entries=entries;this.downloads=downloads;this.devices=devices;}
    @Override public Mono<CacheEntryView> put(UUID user,CreateCacheEntryRequest req){return devices.isUsable(user, req.deviceId()).filter(Boolean.TRUE::equals).switchIfEmpty(Mono.error(new ConflictException("Device 不存在或已撤销"))).flatMap(ignored->{Instant now=Instant.now();return entries.save(new OfflineCacheEntryEntity(null,user,req.deviceId(),req.resourceId(),req.attachmentId(),req.sizeBytes(),req.contentFingerprint(),CacheEntryState.ACTIVE,now,now,now,null));}).map(this::view);}
    @Override public Flux<CacheEntryView> list(UUID user,UUID device){return entries.findAllByUserIdAndDeviceIdOrderByLastAccessedAtDesc(user,device).take(100).filter(e->e.state()==CacheEntryState.ACTIVE).map(this::view);}
    @Override public Mono<CacheEntryView> touch(UUID user,UUID id){return owned(user,id).flatMap(e->entries.save(new OfflineCacheEntryEntity(e.id(),e.userId(),e.deviceId(),e.resourceId(),e.attachmentId(),e.sizeBytes(),e.contentFingerprint(),e.state(),Instant.now(),e.createdAt(),Instant.now(),e.version()))).map(this::view);}
    @Override public Mono<Void> evict(UUID user,UUID id){return owned(user,id).flatMap(e->entries.save(new OfflineCacheEntryEntity(e.id(),e.userId(),e.deviceId(),e.resourceId(),e.attachmentId(),e.sizeBytes(),e.contentFingerprint(),CacheEntryState.EVICTED,e.lastAccessedAt(),e.createdAt(),Instant.now(),e.version()))).then();}
    @Override public Mono<OfflineCacheEvictionView> evictEligible(UUID user, UUID device) {
        return devices.isUsable(user, device).filter(Boolean.TRUE::equals)
            .switchIfEmpty(Mono.error(new ConflictException("Device 不存在或已撤销")))
            .then(Mono.defer(() -> protectedDownloadKeys(user, device)))
            .flatMap(protectedKeys -> entries.findAllByUserIdAndDeviceIdOrderByLastAccessedAtDesc(user, device)
                .filter(e -> e.state() == CacheEntryState.ACTIVE)
                .filter(e -> !protectedKeys.contains(new CacheKey(e.resourceId(), e.attachmentId())))
                .collectList()
                .flatMap(eligible -> evictEntries(eligible).thenReturn(new OfflineCacheEvictionView(device,
                    eligible.size(), eligible.stream().mapToLong(OfflineCacheEntryEntity::sizeBytes).sum(), protectedKeys.size()))));
    }
    private Mono<List<CacheKey>> protectedDownloadKeys(UUID user, UUID device) {
        return downloads.findAllByUserIdAndDeviceIdOrderByCreatedAtDesc(user, device)
            .filter(i -> i.kind() == OfflineCopyKind.DOWNLOAD && i.state() != DownloadState.REMOVED)
            .map(i -> new CacheKey(i.resourceId(), i.attachmentId())).distinct().collectList();
    }
    private Mono<Void> evictEntries(List<OfflineCacheEntryEntity> eligible) {
        return Flux.fromIterable(eligible).flatMap(e -> entries.save(new OfflineCacheEntryEntity(e.id(), e.userId(),
            e.deviceId(), e.resourceId(), e.attachmentId(), e.sizeBytes(), e.contentFingerprint(), CacheEntryState.EVICTED,
            e.lastAccessedAt(), e.createdAt(), Instant.now(), e.version()))).then();
    }
    private record CacheKey(UUID resourceId, UUID attachmentId) { @Override public boolean equals(Object o){return o instanceof CacheKey k&&Objects.equals(resourceId,k.resourceId)&&Objects.equals(attachmentId,k.attachmentId);} @Override public int hashCode(){return Objects.hash(resourceId,attachmentId);} }
    private Mono<OfflineCacheEntryEntity> owned(UUID user,UUID id){return entries.findById(id).filter(e->e.userId().equals(user)).switchIfEmpty(Mono.error(new NotFoundException("Cache Entry 不存在")));}
    private CacheEntryView view(OfflineCacheEntryEntity e){return new CacheEntryView(e.id(),e.userId(),e.deviceId(),e.resourceId(),e.attachmentId(),e.sizeBytes(),e.contentFingerprint(),e.state(),e.lastAccessedAt(),e.createdAt(),e.updatedAt());}
}
