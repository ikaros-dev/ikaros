package run.ikaros.drive;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.transaction.reactive.TransactionalOperator;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.UuidV7Generator;

@Primary
@Service
public class PersistentDriveService implements DriveService {
    private final DriveSpaceRepository spaces;
    private final DriveNodeRepository nodes;
    private final DriveFileRevisionRepository revisions;
    private final DriveChangeRepository changes;
    private final DriveQuotaRepository quotaRepository;
    private final DriveQuotaReservationRepository reservationRepository;
    private final SyncBindingRepository bindingRepository;
    private final SyncConflictRepository conflictRepository;
    private final DeviceRepository deviceRepository;
    private final SyncMappingRepository mappingRepository;
    private final DriveTombstoneRepository tombstoneRepository;
    private final CameraBackupRepository cameraBackupRepository;
    private final TransactionalOperator transactionalOperator;
    private final UuidV7Generator ids = new UuidV7Generator();
    public PersistentDriveService(DriveSpaceRepository spaces, DriveNodeRepository nodes, DriveFileRevisionRepository revisions, DriveChangeRepository changes, DriveQuotaRepository quotaRepository, DriveQuotaReservationRepository reservationRepository, SyncBindingRepository bindingRepository, SyncConflictRepository conflictRepository, DeviceRepository deviceRepository, SyncMappingRepository mappingRepository, DriveTombstoneRepository tombstoneRepository, CameraBackupRepository cameraBackupRepository, TransactionalOperator transactionalOperator) { this.spaces = spaces; this.nodes = nodes; this.revisions = revisions; this.changes = changes; this.quotaRepository = quotaRepository; this.reservationRepository = reservationRepository; this.bindingRepository = bindingRepository; this.conflictRepository = conflictRepository; this.deviceRepository = deviceRepository; this.mappingRepository = mappingRepository; this.tombstoneRepository = tombstoneRepository; this.cameraBackupRepository = cameraBackupRepository; this.transactionalOperator = transactionalOperator; }
    @Override public Mono<DriveSpaceView> createSpace(UUID actor, CreateDriveSpaceRequest req) {
        Instant now = Instant.now(); UUID sid = ids.next(); UUID root = ids.next();
        DriveSpaceEntity draft = new DriveSpaceEntity(sid,actor,req.displayName().trim(),null,0,"ACTIVE",now,now,null);
        DriveNodeEntity rootNode = new DriveNodeEntity(root,sid,null,DriveNodeType.FOLDER,"My Drive","my drive",DriveLifecycle.ACTIVE,null,actor,now,now,null,0,null);
        return transactionalOperator.transactional(spaces.save(draft).flatMap(saved -> nodes.save(rootNode).then(spaces.save(new DriveSpaceEntity(sid,actor,saved.displayName(),root,0,"ACTIVE",now,Instant.now(),saved.version()))).then(quotaRepository.save(new DriveQuotaEntity(sid,100L*1024*1024*1024,0,0,null)))).then(spaces.findById(sid)).map(this::view));
    }
    @Override public Flux<DriveSpaceView> listSpaces(UUID actor) { return spaces.findAllByOwnerUserIdOrderByCreatedAtAsc(actor).map(this::view); }
    @Override public Flux<DriveNodeView> children(UUID actor, UUID sid, UUID parent) { return ownedSpace(actor,sid).flatMapMany(s -> nodes.findAllByDriveSpaceIdAndParentIdAndLifecycleOrderByNormalizedNameAsc(sid,parent,DriveLifecycle.ACTIVE).map(this::view)); }
    @Override public Mono<DriveNodeView> createNode(UUID actor, UUID sid, CreateDriveNodeRequest req) {
        return transactionalOperator.transactional(ownedSpace(actor,sid).flatMap(s -> { UUID parent = req.parentId()==null?s.rootNodeId():req.parentId(); return nodes.findByIdAndDriveSpaceId(parent,sid).switchIfEmpty(Mono.error(new NotFoundException("父节点不存在"))).flatMap(p -> {
            if (p.nodeType()!=DriveNodeType.FOLDER) return Mono.error(new ConflictException("父节点不是目录")); Instant now=Instant.now();
            DriveNodeEntity n=new DriveNodeEntity(null,sid,parent,req.nodeType(),req.name().trim(),normalize(req.name()),DriveLifecycle.ACTIVE,null,actor,now,now,null,0,null);
            return nodes.save(n).onErrorMap(DuplicateKeyException.class,e->new ConflictException("同目录下名称已存在")).flatMap(saved -> record(s,saved,DriveMutationKind.NODE_CREATED,null).thenReturn(view(saved)));
        }); }));
    }
    @Override public Mono<DriveNodeView> rename(UUID actor, UUID id, RenameDriveNodeRequest req) { return transactionalOperator.transactional(ownedNode(actor,id).flatMap(n -> ownedSpace(actor,n.driveSpaceId()).flatMap(space -> { check(n.nodeVersion(),req.expectedVersion()); DriveNodeEntity c=new DriveNodeEntity(n.id(),n.driveSpaceId(),n.parentId(),n.nodeType(),req.name().trim(),normalize(req.name()),n.lifecycle(),n.currentRevisionId(),n.createdBy(),n.createdAt(),Instant.now(),n.trashedAt(),n.nodeVersion()+1,n.version()); return nodes.save(c).onErrorMap(DuplicateKeyException.class,e->new ConflictException("同目录下名称已存在")).flatMap(saved->record(space,saved,DriveMutationKind.NODE_RENAMED,null).thenReturn(view(saved))); })) ); }
    @Override public Mono<DriveNodeView> move(UUID actor, UUID id, MoveDriveNodeRequest req) { return transactionalOperator.transactional(ownedNode(actor,id).flatMap(n -> nodes.findByIdAndDriveSpaceId(req.parentId(),n.driveSpaceId()).switchIfEmpty(Mono.error(new NotFoundException("目标父节点不存在"))).flatMap(p -> { check(n.nodeVersion(),req.expectedVersion()); if(p.nodeType()!=DriveNodeType.FOLDER||p.id().equals(n.id())) return Mono.error(new ConflictException("目标父节点无效")); DriveNodeEntity c=new DriveNodeEntity(n.id(),n.driveSpaceId(),p.id(),n.nodeType(),n.name(),n.normalizedName(),n.lifecycle(),n.currentRevisionId(),n.createdBy(),n.createdAt(),Instant.now(),n.trashedAt(),n.nodeVersion()+1,n.version()); return nodes.save(c).onErrorMap(DuplicateKeyException.class,e->new ConflictException("目标目录下名称已存在")).flatMap(saved -> ownedSpace(actor, saved.driveSpaceId()).flatMap(space -> record(space, saved, DriveMutationKind.NODE_MOVED, null).thenReturn(view(saved)))); }))); }
    @Override public Mono<DriveNodeView> trash(UUID actor, UUID id, long expected) { return lifecycle(actor,id,expected,DriveLifecycle.TRASHED); }
    @Override public Mono<DriveNodeView> restore(UUID actor, UUID id, long expected) { return lifecycle(actor,id,expected,DriveLifecycle.ACTIVE); }
    @Override
    public Mono<DriveRevisionView> createRevision(UUID actor, UUID id, CreateDriveRevisionRequest req) {
        return transactionalOperator.transactional(ownedNode(actor, id).flatMap(node -> {
            if (node.nodeType() != DriveNodeType.FILE) {
                return Mono.error(new ConflictException("只有文件节点可以创建版本"));
            }
            check(node.nodeVersion(), req.expectedNodeVersion());
            Mono<DriveFileRevisionEntity> existing = req.operationId() == null
                ? Mono.empty() : revisions.findByFileNodeIdAndOperationId(id, req.operationId());
            return existing.switchIfEmpty(Mono.defer(() -> revisions
                .findAllByFileNodeIdOrderByRevisionNoDesc(id).take(1).collectList()
                .flatMap(previous -> {
                    long number = previous.isEmpty() ? 1 : previous.get(0).revisionNo() + 1;
                    Instant now = Instant.now();
                    return revisions.save(new DriveFileRevisionEntity(null, id, number, req.attachmentId(),
                        req.contentFingerprint(), null, actor, req.operationId(), now, null));
                })))
                .flatMap(saved -> {
                    if (node.currentRevisionId() != null && saved.id().equals(node.currentRevisionId())) {
                        return Mono.just(view(saved));
                    }
                    DriveNodeEntity changed = new DriveNodeEntity(node.id(), node.driveSpaceId(), node.parentId(),
                        node.nodeType(), node.name(), node.normalizedName(), node.lifecycle(), saved.id(),
                        node.createdBy(), node.createdAt(), Instant.now(), node.trashedAt(), node.nodeVersion() + 1,
                        node.version());
                    return nodes.save(changed)
                        .flatMap(updated -> ownedSpace(actor, updated.driveSpaceId())
                            .flatMap(space -> record(space, updated, DriveMutationKind.CONTENT_REVISION_CREATED, saved.id())
                                .thenReturn(view(saved))));
                });
        }));
    }
    @Override public Flux<DriveRevisionView> revisions(UUID actor, UUID id) { return ownedNode(actor,id).flatMapMany(n->revisions.findAllByFileNodeIdOrderByRevisionNoDesc(id).map(this::view)); }
    @Override public Flux<DriveChangeView> changes(UUID actor, UUID sid, long afterSequence) { return ownedSpace(actor,sid).flatMapMany(s->changes.findAllByDriveSpaceIdAndSequenceGreaterThanOrderBySequenceAsc(sid,afterSequence).map(this::changeView)); }
    @Override public Mono<DriveQuotaView> quota(UUID actor, UUID sid) { return ownedSpace(actor,sid).then(quotaRepository.findById(sid).switchIfEmpty(Mono.error(new NotFoundException("Quota 不存在"))).map(q->new DriveQuotaView(sid,q.limitBytes(),q.usedBytes(),q.reservedBytes(),q.limitBytes()-q.usedBytes()-q.reservedBytes()))); }
    @Override
    public Mono<DriveQuotaReservationView> beginUpload(UUID actor, UUID sid, BeginDriveUploadRequest req) {
        return transactionalOperator.transactional(ownedSpace(actor, sid)
            .then(reservationRepository.findByDriveSpaceIdAndUploadSessionId(sid, req.uploadSessionId())
                .map(this::reservationView))
            .switchIfEmpty(quotaRepository.findById(sid)
                .switchIfEmpty(Mono.error(new NotFoundException("Quota 不存在")))
                .flatMap(q -> {
                    long available = q.limitBytes() - q.usedBytes() - q.reservedBytes();
                    if (req.reservedBytes() > available) return Mono.error(new ConflictException("配额不足"));
                    Instant now = Instant.now();
                    DriveQuotaReservationEntity reservation = new DriveQuotaReservationEntity(null, sid,
                        req.uploadSessionId(), req.reservedBytes(), QuotaReservationState.ACTIVE,
                        now.plusSeconds(3600), now, now, null);
                    DriveQuotaEntity updated = new DriveQuotaEntity(sid, q.limitBytes(), q.usedBytes(),
                        q.reservedBytes() + req.reservedBytes(), q.version());
                    return quotaRepository.save(updated).then(reservationRepository.save(reservation))
                        .map(this::reservationView);
                })));
    }
    @Override public Mono<DriveQuotaReservationView> finalizeUpload(UUID actor, UUID sid, UUID rid) { return settle(actor,sid,rid,QuotaReservationState.COMMITTED,true); }
    @Override public Mono<DriveQuotaReservationView> abortUpload(UUID actor, UUID sid, UUID rid) { return settle(actor,sid,rid,QuotaReservationState.RELEASED,false); }
    private Mono<DriveQuotaReservationView> settle(UUID actor, UUID sid, UUID rid,
                                                   QuotaReservationState target, boolean commit) {
        return transactionalOperator.transactional(ownedSpace(actor, sid)
            .then(reservationRepository.findById(rid).filter(r -> r.driveSpaceId().equals(sid))
                .switchIfEmpty(Mono.error(new NotFoundException("配额 reservation 不存在")))
                .flatMap(reservation -> {
                    if (reservation.state() != QuotaReservationState.ACTIVE) {
                        return Mono.just(reservationView(reservation));
                    }
                    return quotaRepository.findById(sid).flatMap(q -> {
                        DriveQuotaEntity updated = new DriveQuotaEntity(sid, q.limitBytes(),
                            commit ? q.usedBytes() + reservation.reservedBytes() : q.usedBytes(),
                            q.reservedBytes() - reservation.reservedBytes(), q.version());
                        DriveQuotaReservationEntity completed = new DriveQuotaReservationEntity(
                            reservation.id(), sid, reservation.uploadSessionId(), reservation.reservedBytes(), target,
                            reservation.expiresAt(), reservation.createdAt(), Instant.now(), reservation.version());
                        return quotaRepository.save(updated).then(reservationRepository.save(completed))
                            .map(this::reservationView);
                    });
                })));
    }
    private Mono<DriveNodeView> lifecycle(UUID actor,UUID id,long expected,DriveLifecycle state){return transactionalOperator.transactional(ownedNode(actor,id).flatMap(n->ownedSpace(actor,n.driveSpaceId()).flatMap(space->{check(n.nodeVersion(),expected); if(n.lifecycle()==DriveLifecycle.PURGED)return Mono.error(new ConflictException("节点已永久删除")); Instant now=Instant.now(); DriveNodeEntity c=new DriveNodeEntity(n.id(),n.driveSpaceId(),n.parentId(),n.nodeType(),n.name(),n.normalizedName(),state,n.currentRevisionId(),n.createdBy(),n.createdAt(),now,state==DriveLifecycle.TRASHED?now:null,n.nodeVersion()+1,n.version()); DriveMutationKind kind=state==DriveLifecycle.TRASHED?DriveMutationKind.NODE_TRASHED:DriveMutationKind.NODE_RESTORED; return nodes.save(c).flatMap(saved->advance(space,saved,kind,null).flatMap(updated->state==DriveLifecycle.TRASHED?tombstoneRepository.save(new DriveTombstoneEntity(null,saved.driveSpaceId(),saved.id(),updated.changeGeneration(),saved.nodeVersion(),TombstoneLifecycle.TRASHED,saved.parentId(),saved.name(),now,now.plusSeconds(30L*24*3600))).thenReturn(saved):Mono.just(saved))).map(this::view);})));}
    private Mono<DriveSpaceEntity> ownedSpace(UUID actor,UUID id){return spaces.findById(id).filter(s->s.ownerUserId().equals(actor)).switchIfEmpty(Mono.error(new NotFoundException("Drive Space 不存在")));}
    private Mono<DriveNodeEntity> ownedNode(UUID actor,UUID id){return nodes.findById(id).switchIfEmpty(Mono.error(new NotFoundException("Drive Node 不存在"))).flatMap(n->ownedSpace(actor,n.driveSpaceId()).thenReturn(n));}
    private Mono<DriveSpaceEntity> advance(DriveSpaceEntity s){return spaces.save(new DriveSpaceEntity(s.id(),s.ownerUserId(),s.displayName(),s.rootNodeId(),s.changeGeneration()+1,s.state(),s.createdAt(),Instant.now(),s.version()));}
    private Mono<DriveSpaceEntity> advance(DriveSpaceEntity s, DriveNodeEntity node, DriveMutationKind kind, UUID revisionId) { long sequence=s.changeGeneration()+1; Instant now=Instant.now(); return spaces.save(new DriveSpaceEntity(s.id(),s.ownerUserId(),s.displayName(),s.rootNodeId(),sequence,s.state(),s.createdAt(),now,s.version())).flatMap(saved->changes.save(new DriveChangeEntity(null,s.id(),sequence,node.id(),kind,node.nodeVersion(),revisionId,now)).thenReturn(saved)); }
    private Mono<Void> record(DriveSpaceEntity space, DriveNodeEntity node, DriveMutationKind kind, UUID revisionId) {
        return advance(space, node, kind, revisionId).then();
    }
    private void check(long actual,long expected){if(actual!=expected)throw new ConflictException("Drive Node 版本冲突");}
    private String normalize(String value){return Normalizer.normalize(value.trim(),Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);}
    private DriveSpaceView view(DriveSpaceEntity s){return new DriveSpaceView(s.id(),s.ownerUserId(),s.displayName(),s.rootNodeId(),s.changeGeneration(),s.createdAt(),s.updatedAt(),s.version()==null?0:s.version());}
    private DriveNodeView view(DriveNodeEntity n){return new DriveNodeView(n.id(),n.driveSpaceId(),n.parentId(),n.nodeType(),n.name(),n.normalizedName(),n.lifecycle(),n.currentRevisionId(),n.nodeVersion(),n.createdAt(),n.updatedAt());}
    private DriveRevisionView view(DriveFileRevisionEntity r){return new DriveRevisionView(r.id(),r.fileNodeId(),r.revisionNo(),r.attachmentId(),r.contentFingerprint(),r.contentModifiedAt(),r.createdAt(),r.createdBy());}
    private DriveChangeView changeView(DriveChangeEntity c){return new DriveChangeView(c.id(),c.driveSpaceId(),c.sequence(),c.nodeId(),c.mutationKind(),c.nodeVersion(),c.revisionId(),c.occurredAt());}
    private DriveQuotaReservationView reservationView(DriveQuotaReservationEntity r){return new DriveQuotaReservationView(r.id(),r.driveSpaceId(),r.uploadSessionId(),r.reservedBytes(),r.state(),r.expiresAt());}
    @Override
    public Mono<SyncBindingView> createBinding(UUID actor, CreateSyncBindingRequest req) {
        return deviceRepository.findById(req.deviceId())
            .filter(device -> device.userId().equals(actor) && device.trustState() != DeviceTrustState.REVOKED)
            .switchIfEmpty(Mono.error(new ConflictException("Device 不存在或已撤销")))
            .then(ownedSpace(actor, req.driveSpaceId()))
            .flatMap(space ->
            nodes.findByIdAndDriveSpaceId(req.remoteRootNodeId(), req.driveSpaceId())
                .filter(n -> n.nodeType() == DriveNodeType.FOLDER && n.lifecycle() == DriveLifecycle.ACTIVE)
                .switchIfEmpty(Mono.error(new ConflictException("远端同步根目录无效")))
                .then(bindingRepository.findAllByUserIdAndDeviceIdAndEnabledTrue(actor, req.deviceId())
                    .collectList())
                .flatMap(existing -> {
                    String local = normalizeScope(req.localScopeId());
                    boolean write = req.mode() == SyncMode.TWO_WAY || req.mode() == SyncMode.UPLOAD_ONLY
                        || req.mode() == SyncMode.BACKUP;
                    boolean overlap = existing.stream().anyMatch(binding -> write
                        && (binding.mode() == SyncMode.TWO_WAY || binding.mode() == SyncMode.UPLOAD_ONLY
                            || binding.mode() == SyncMode.BACKUP)
                        && scopesOverlap(local, normalizeScope(binding.localScopeId())));
                    if (overlap) return Mono.error(new ConflictException("设备本地同步 Scope 重叠"));
                    Instant now = Instant.now();
                    SyncBindingEntity binding = new SyncBindingEntity(null, actor, req.deviceId(), req.driveSpaceId(),
                        req.remoteRootNodeId(), req.localScopeId(), req.localDisplayPath(), req.sourceKind(), req.mode(),
                        req.deletePolicy() == null ? DeletePolicy.KEEP_REMOTE : req.deletePolicy(),
                        req.conflictPolicy() == null ? ConflictPolicy.PRESERVE_BOTH : req.conflictPolicy(),
                        true, SyncBindingState.ACTIVE, 0, now, now, null);
                    return bindingRepository.save(binding).map(this::bindingView);
                }));
    }
    @Override public Flux<SyncBindingView> bindings(UUID actor) { return bindingRepository.findAllByUserIdOrderByCreatedAtAsc(actor).map(this::bindingView); }
    @Override
    public Mono<SyncBindingView> setBindingEnabled(UUID actor, UUID id, boolean enabled) {
        return bindingRepository.findById(id)
            .filter(binding -> binding.userId().equals(actor))
            .switchIfEmpty(Mono.error(new NotFoundException("Sync Binding 不存在")))
            .flatMap(binding -> bindingRepository.save(new SyncBindingEntity(binding.id(), binding.userId(),
                binding.deviceId(), binding.driveSpaceId(), binding.remoteRootNodeId(), binding.localScopeId(),
                binding.localDisplayPath(), binding.sourceKind(), binding.mode(), binding.deletePolicy(),
                binding.conflictPolicy(), enabled, enabled ? SyncBindingState.ACTIVE : SyncBindingState.PAUSED,
                binding.cursor(), binding.createdAt(), Instant.now(), binding.version())))
            .map(this::bindingView);
    }
    private String normalizeScope(String value){return value.trim().replace('\\','/').replaceAll("/+","/").toLowerCase(java.util.Locale.ROOT);}
    private boolean scopesOverlap(String a,String b){return a.equals(b)||a.startsWith(b.endsWith("/")?b:b+"/")||b.startsWith(a.endsWith("/")?a:a+"/");}
    private SyncBindingView bindingView(SyncBindingEntity b){return new SyncBindingView(b.id(),b.userId(),b.deviceId(),b.driveSpaceId(),b.remoteRootNodeId(),b.localScopeId(),b.localDisplayPath(),b.sourceKind(),b.mode(),b.deletePolicy(),b.conflictPolicy(),b.enabled(),b.state(),b.cursor(),b.createdAt(),b.updatedAt());}
    @Override public Mono<SyncConflictView> createConflict(UUID actor, CreateSyncConflictRequest req) { return bindingRepository.findById(req.bindingId()).filter(b->b.userId().equals(actor)).switchIfEmpty(Mono.error(new NotFoundException("Sync Binding 不存在"))).flatMap(binding->ownedNode(actor,req.nodeId()).then(Mono.defer(()->{Instant now=Instant.now();return conflictRepository.save(new SyncConflictEntity(null,req.bindingId(),req.nodeId(),req.baseRevisionId(),req.remoteRevisionId(),req.localFingerprint(),SyncConflictState.OPEN,now,null,null,null));}))).map(this::conflictView); }
    @Override public Flux<SyncConflictView> conflicts(UUID actor, UUID bindingId) { return bindingRepository.findById(bindingId).filter(b->b.userId().equals(actor)).switchIfEmpty(Mono.error(new NotFoundException("Sync Binding 不存在"))).flatMapMany(b->conflictRepository.findAllByBindingIdOrderByDetectedAtDesc(bindingId).map(this::conflictView)); }
    @Override public Mono<SyncConflictView> resolveConflict(UUID actor, UUID id, SyncConflictState state) { if(state!=SyncConflictState.RESOLVED&&state!=SyncConflictState.DISMISSED)return Mono.error(new ConflictException("Conflict 只能进入 RESOLVED 或 DISMISSED")); return conflictRepository.findById(id).switchIfEmpty(Mono.error(new NotFoundException("Conflict 不存在"))).flatMap(c->bindingRepository.findById(c.bindingId()).filter(b->b.userId().equals(actor)).switchIfEmpty(Mono.error(new NotFoundException("Conflict 不存在"))).then(conflictRepository.save(new SyncConflictEntity(c.id(),c.bindingId(),c.nodeId(),c.baseRevisionId(),c.remoteRevisionId(),c.localFingerprint(),state,c.detectedAt(),Instant.now(),actor,c.version())))).map(this::conflictView); }
    private SyncConflictView conflictView(SyncConflictEntity c){return new SyncConflictView(c.id(),c.bindingId(),c.nodeId(),c.baseRevisionId(),c.remoteRevisionId(),c.localFingerprint(),c.state(),c.detectedAt(),c.resolvedAt(),c.resolvedBy());}
    @Override public Mono<DeviceView> registerDevice(UUID actor, RegisterDeviceRequest req) { return deviceRepository.findByUserIdAndInstallationId(actor,req.installationId()).flatMap(existing->deviceRepository.save(new DeviceEntity(existing.id(),actor,existing.installationId(),req.displayName().trim(),req.platform(),req.appVersion(),existing.trustState(),existing.registeredAt(),Instant.now(),existing.revokedAt(),existing.version()))).switchIfEmpty(Mono.defer(()->{Instant now=Instant.now();return deviceRepository.save(new DeviceEntity(null,actor,req.installationId(),req.displayName().trim(),req.platform(),req.appVersion(),DeviceTrustState.ACTIVE,now,now,null,null));})).map(this::deviceView); }
    @Override public Flux<DeviceView> devices(UUID actor) { return deviceRepository.findAllByUserIdOrderByRegisteredAtAsc(actor).map(this::deviceView); }
    @Override public Mono<DeviceView> revokeDevice(UUID actor, UUID id) { return deviceRepository.findById(id).filter(d->d.userId().equals(actor)).switchIfEmpty(Mono.error(new NotFoundException("Device 不存在"))).flatMap(d->deviceRepository.save(new DeviceEntity(d.id(),d.userId(),d.installationId(),d.displayName(),d.platform(),d.appVersion(),DeviceTrustState.REVOKED,d.registeredAt(),d.lastSeenAt(),Instant.now(),d.version()))).map(this::deviceView); }
    private DeviceView deviceView(DeviceEntity d){return new DeviceView(d.id(),d.userId(),d.installationId(),d.displayName(),d.platform(),d.appVersion(),d.trustState(),d.registeredAt(),d.lastSeenAt(),d.revokedAt());}
    @Override public Mono<SyncMappingView> upsertMapping(UUID actor, UUID bindingId, UpsertSyncMappingRequest req) { return bindingRepository.findById(bindingId).filter(b->b.userId().equals(actor)&&b.enabled()).switchIfEmpty(Mono.error(new NotFoundException("Sync Binding 不存在或已暂停"))).flatMap(binding->nodes.findByIdAndDriveSpaceId(req.remoteNodeId(),binding.driveSpaceId()).switchIfEmpty(Mono.error(new NotFoundException("Remote Node 不存在"))).then(mappingRepository.findByBindingIdAndLocalItemId(bindingId,req.localItemId()))).flatMap(existing->{Instant now=Instant.now();return mappingRepository.save(new SyncMappingEntity(existing.id(),bindingId,req.localItemId(),req.remoteNodeId(),req.lastSyncedRevisionId(),req.lastSyncedFingerprint(),req.lastSeenRemoteVersion(),req.state()==null?SyncMappingState.ACTIVE:req.state(),now,existing.version()));}).switchIfEmpty(Mono.defer(()->{Instant now=Instant.now();return mappingRepository.save(new SyncMappingEntity(null,bindingId,req.localItemId(),req.remoteNodeId(),req.lastSyncedRevisionId(),req.lastSyncedFingerprint(),req.lastSeenRemoteVersion(),req.state()==null?SyncMappingState.ACTIVE:req.state(),now,null));})).map(this::mappingView); }
    @Override public Flux<SyncMappingView> mappings(UUID actor, UUID bindingId) { return bindingRepository.findById(bindingId).filter(b->b.userId().equals(actor)).switchIfEmpty(Mono.error(new NotFoundException("Sync Binding 不存在"))).flatMapMany(b->mappingRepository.findAllByBindingIdOrderByUpdatedAtAsc(bindingId).map(this::mappingView)); }
    private SyncMappingView mappingView(SyncMappingEntity m){return new SyncMappingView(m.id(),m.bindingId(),m.localItemId(),m.remoteNodeId(),m.lastSyncedRevisionId(),m.lastSyncedFingerprint(),m.lastSeenRemoteVersion(),m.state(),m.updatedAt());}
    @Override public Flux<DriveTombstoneView> tombstones(UUID actor, UUID sid, long afterSequence) { return ownedSpace(actor,sid).flatMapMany(s->tombstoneRepository.findAllByDriveSpaceIdAndSequenceGreaterThanOrderBySequenceAsc(sid,afterSequence).map(this::tombstoneView)); }
    private DriveTombstoneView tombstoneView(DriveTombstoneEntity t){return new DriveTombstoneView(t.id(),t.driveSpaceId(),t.nodeId(),t.sequence(),t.nodeVersion(),t.lifecycle(),t.previousParentId(),t.previousName(),t.deletedAt(),t.retentionDeadline());}
    @Override public Mono<CameraBackupView> updateCameraBackup(UUID actor, UUID bindingId, CameraBackupRequest req) { return bindingRepository.findById(bindingId).filter(b->b.userId().equals(actor)&&b.mode()==SyncMode.BACKUP).switchIfEmpty(Mono.error(new NotFoundException("Backup Binding 不存在"))).flatMap(binding->cameraBackupRepository.findByBindingIdAndSourceItemId(bindingId,req.sourceItemId())).flatMap(existing->{if(existing.state()==CameraBackupState.BACKUP_VERIFIED&&req.state()==CameraBackupState.ERROR)return Mono.error(new ConflictException("已验证备份不能降级为错误"));if(existing.state()==CameraBackupState.REMOVED_AFTER_VERIFIED_BACKUP&&req.state()!=CameraBackupState.REMOVED_AFTER_VERIFIED_BACKUP)return Mono.error(new ConflictException("已释放本地空间的备份不能重新排队"));return cameraBackupRepository.save(new CameraBackupEntity(existing.id(),bindingId,req.sourceItemId(),req.state(),req.remoteNodeId(),req.remoteRevisionId(),req.contentFingerprint(),req.errorMessage(),Instant.now(),existing.version()));}).switchIfEmpty(Mono.defer(()->cameraBackupRepository.save(new CameraBackupEntity(null,bindingId,req.sourceItemId(),req.state(),req.remoteNodeId(),req.remoteRevisionId(),req.contentFingerprint(),req.errorMessage(),Instant.now(),null)))).map(this::cameraView); }
    @Override public Flux<CameraBackupView> cameraBackups(UUID actor, UUID bindingId) { return bindingRepository.findById(bindingId).filter(b->b.userId().equals(actor)).switchIfEmpty(Mono.error(new NotFoundException("Sync Binding 不存在"))).flatMapMany(b->cameraBackupRepository.findAllByBindingIdOrderByUpdatedAtAsc(bindingId).map(this::cameraView)); }
    private CameraBackupView cameraView(CameraBackupEntity c){return new CameraBackupView(c.id(),c.bindingId(),c.sourceItemId(),c.state(),c.remoteNodeId(),c.remoteRevisionId(),c.contentFingerprint(),c.errorMessage(),c.updatedAt());}
    @Override public Flux<SyncMutationResult> applyMutations(UUID actor, UUID bindingId, java.util.List<SyncMutationRequest> requests) { return bindingRepository.findById(bindingId).filter(b->b.userId().equals(actor)&&b.enabled()).switchIfEmpty(Mono.error(new NotFoundException("Sync Binding 不存在或已暂停"))).thenMany(Flux.fromIterable(requests).concatMap(request->{Mono<DriveNodeView> action=switch(request.kind()){case RENAME->rename(actor,request.nodeId(),new RenameDriveNodeRequest(request.name(),request.expectedVersion()));case MOVE->move(actor,request.nodeId(),new MoveDriveNodeRequest(request.parentId(),request.expectedVersion()));case TRASH->trash(actor,request.nodeId(),request.expectedVersion());case RESTORE->restore(actor,request.nodeId(),request.expectedVersion());};return action.map(node->new SyncMutationResult(request.operationId(),true,node,null,null)).onErrorResume(error->Mono.just(new SyncMutationResult(request.operationId(),false,null,error.getClass().getSimpleName(),error.getMessage())));})); }
    @Override public Mono<SyncBindingView> advanceCursor(UUID actor, UUID id, long cursor) { return bindingRepository.findById(id).filter(b->b.userId().equals(actor)&&b.enabled()).switchIfEmpty(Mono.error(new NotFoundException("Sync Binding 不存在或已暂停"))).flatMap(b->{if(cursor<b.cursor())return Mono.error(new ConflictException("Sync Cursor 不能回退"));return bindingRepository.save(new SyncBindingEntity(b.id(),b.userId(),b.deviceId(),b.driveSpaceId(),b.remoteRootNodeId(),b.localScopeId(),b.localDisplayPath(),b.sourceKind(),b.mode(),b.deletePolicy(),b.conflictPolicy(),b.enabled(),b.state(),cursor,b.createdAt(),java.time.Instant.now(),b.version()));}).map(this::bindingView); }
    @Override public Mono<SyncBindingView> requestFullResync(UUID actor, UUID id) { return bindingRepository.findById(id).filter(b->b.userId().equals(actor)).switchIfEmpty(Mono.error(new NotFoundException("Sync Binding 不存在"))).flatMap(b->bindingRepository.save(new SyncBindingEntity(b.id(),b.userId(),b.deviceId(),b.driveSpaceId(),b.remoteRootNodeId(),b.localScopeId(),b.localDisplayPath(),b.sourceKind(),b.mode(),b.deletePolicy(),b.conflictPolicy(),b.enabled(),SyncBindingState.DEGRADED,0,b.createdAt(),java.time.Instant.now(),b.version()))).map(this::bindingView); }
}
