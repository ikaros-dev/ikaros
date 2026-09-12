package run.ikaros.offline;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.sync.api.DeviceTrustQuery;
@Service
public class PersistentDownloadService implements DownloadService {
    private final DownloadIntentRepository repository;
    private final OfflineCacheEntryRepository cacheEntries;
    private final DeviceTrustQuery devices;
    public PersistentDownloadService(DownloadIntentRepository repository, OfflineCacheEntryRepository cacheEntries, DeviceTrustQuery devices) { this.repository=repository; this.cacheEntries=cacheEntries; this.devices=devices; }
    @Override public Mono<DownloadView> create(UUID user, CreateDownloadRequest req) { return devices.isUsable(user, req.deviceId()).filter(Boolean.TRUE::equals).switchIfEmpty(Mono.error(new ConflictException("Device 不存在或已撤销"))).flatMap(ignored -> reusableCache(user, req).flatMap(reused -> saveIntent(user, req, reused ? DownloadState.COMPLETED : DownloadState.QUEUED))).onErrorMap(DuplicateKeyException.class,e->new ConflictException("Download 已存在")).map(this::view); }
    private Mono<Boolean> reusableCache(UUID user, CreateDownloadRequest req) { if (req.kind() == OfflineCopyKind.CACHE) return Mono.just(false); Mono<OfflineCacheEntryEntity> match = req.attachmentId() == null ? cacheEntries.findFirstByUserIdAndDeviceIdAndResourceIdAndAttachmentIdIsNullAndState(user, req.deviceId(), req.resourceId(), CacheEntryState.ACTIVE) : cacheEntries.findFirstByUserIdAndDeviceIdAndResourceIdAndAttachmentIdAndState(user, req.deviceId(), req.resourceId(), req.attachmentId(), CacheEntryState.ACTIVE); return match.map(ignored -> true).defaultIfEmpty(false); }
    private Mono<DownloadIntentEntity> saveIntent(UUID user, CreateDownloadRequest req, DownloadState state) { Instant now=Instant.now(); return repository.save(new DownloadIntentEntity(null,user,req.deviceId(),req.resourceId(),req.attachmentId(),req.kind()==null?OfflineCopyKind.DOWNLOAD:req.kind(),state,null,1,now,now,null)); }
    @Override public Flux<DownloadView> list(UUID user, UUID device) { return repository.findAllByUserIdAndDeviceIdOrderByCreatedAtDesc(user,device).take(100).map(this::view); }
    @Override public Mono<DownloadView> updateState(UUID user, UUID id, UpdateDownloadStateRequest req) { return owned(user,id).flatMap(old->{if(!allowed(old.state(),req.state()))return Mono.error(new ConflictException("Download 状态迁移不合法"));return repository.save(new DownloadIntentEntity(old.id(),old.userId(),old.deviceId(),old.resourceId(),old.attachmentId(),old.kind(),req.state(),req.failureReason(),old.manifestVersion(),old.createdAt(),Instant.now(),old.version()));}).map(this::view); }
    @Override public Mono<DownloadView> remove(UUID user, UUID id) { return owned(user,id).flatMap(old->repository.save(new DownloadIntentEntity(old.id(),old.userId(),old.deviceId(),old.resourceId(),old.attachmentId(),old.kind(),DownloadState.REMOVED,old.failureReason(),old.manifestVersion(),old.createdAt(),Instant.now(),old.version()))).map(this::view); }
    private Mono<DownloadIntentEntity> owned(UUID user,UUID id){return repository.findById(id).filter(i->i.userId().equals(user)).switchIfEmpty(Mono.error(new NotFoundException("Download 不存在")));}
    private boolean allowed(DownloadState from,DownloadState to){if(from==DownloadState.REMOVED||from==DownloadState.CANCELLED)return false;if(to==DownloadState.COMPLETED&&from!=DownloadState.VERIFYING)return false;return true;}
    private DownloadView view(DownloadIntentEntity i){return new DownloadView(i.id(),i.userId(),i.deviceId(),i.resourceId(),i.attachmentId(),i.kind(),i.state(),i.failureReason(),i.manifestVersion(),i.createdAt(),i.updatedAt());}
}
