package run.ikaros.drive;

import java.time.Instant;
import java.text.Normalizer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.UuidV7Generator;

@Service
public class DefaultDriveService implements DriveService {
    private record Space(UUID id, UUID owner, String name, UUID root, long generation, Instant created, Instant updated, long version) {}
    private record Node(UUID id, UUID space, UUID parent, DriveNodeType type, String name, String normalized,
        DriveLifecycle lifecycle, UUID revision, long version, Instant created, Instant updated) {}
    private final UuidV7Generator ids = new UuidV7Generator();
    private final ConcurrentMap<UUID, Space> spaces = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Node> nodes = new ConcurrentHashMap<>();

    @Override public Mono<DriveSpaceView> createSpace(UUID actorId, CreateDriveSpaceRequest request) {
        return Mono.fromSupplier(() -> {
            Instant now = Instant.now(); UUID spaceId = ids.next(); UUID root = ids.next();
            spaces.put(spaceId, new Space(spaceId, actorId, request.displayName().trim(), root, 0, now, now, 0));
            nodes.put(root, new Node(root, spaceId, null, DriveNodeType.FOLDER, "My Drive", "my drive",
                DriveLifecycle.ACTIVE, null, 0, now, now));
            return view(spaces.get(spaceId));
        });
    }
    @Override public Flux<DriveSpaceView> listSpaces(UUID actorId) {
        return Flux.fromIterable(spaces.values()).filter(s -> s.owner().equals(actorId)).map(this::view);
    }
    @Override public Flux<DriveNodeView> children(UUID actorId, UUID spaceId, UUID parentId) {
        return ownedSpace(actorId, spaceId).flatMapMany(s -> Flux.fromIterable(nodes.values())
            .filter(n -> n.space().equals(spaceId) && java.util.Objects.equals(n.parent(), parentId)
                && n.lifecycle() == DriveLifecycle.ACTIVE).map(this::view));
    }
    @Override public Mono<DriveNodeView> createNode(UUID actorId, UUID spaceId, CreateDriveNodeRequest request) {
        return ownedSpace(actorId, spaceId).flatMap(space -> {
            UUID parent = request.parentId() == null ? space.root() : request.parentId();
            return requiredNode(parent).flatMap(parentNode -> {
                if (parentNode.space() != spaceId || parentNode.type() != DriveNodeType.FOLDER) return Mono.error(new ConflictException("父节点无效"));
                String normalized = normalize(request.name());
                boolean duplicate = nodes.values().stream().anyMatch(n -> n.space().equals(spaceId)
                    && java.util.Objects.equals(n.parent(), parent) && n.lifecycle() == DriveLifecycle.ACTIVE
                    && n.normalized().equals(normalized));
                if (duplicate) return Mono.error(new ConflictException("同目录下名称已存在"));
                Instant now = Instant.now(); UUID id = ids.next();
                Node node = new Node(id, spaceId, parent, request.nodeType(), request.name().trim(), normalized,
                    DriveLifecycle.ACTIVE, null, 0, now, now); nodes.put(id, node); advance(space); return Mono.just(view(node));
            });
        });
    }
    @Override public Mono<DriveNodeView> rename(UUID actorId, UUID nodeId, RenameDriveNodeRequest request) {
        return ownedNode(actorId, nodeId).flatMap(node -> {
            checkVersion(node, request.expectedVersion()); String normalized = normalize(request.name());
            boolean duplicate = nodes.values().stream().anyMatch(n -> n.space().equals(node.space()) && !n.id().equals(nodeId)
                && java.util.Objects.equals(n.parent(), node.parent()) && n.lifecycle() == DriveLifecycle.ACTIVE
                && n.normalized().equals(normalized));
            if (duplicate) return Mono.error(new ConflictException("同目录下名称已存在"));
            Node changed = new Node(node.id(), node.space(), node.parent(), node.type(), request.name().trim(), normalized,
                node.lifecycle(), node.revision(), node.version() + 1, node.created(), Instant.now()); nodes.put(nodeId, changed);
            advance(spaces.get(node.space())); return Mono.just(view(changed));
        });
    }
    @Override public Mono<DriveNodeView> move(UUID actorId, UUID nodeId, MoveDriveNodeRequest request) {
        return ownedNode(actorId, nodeId).flatMap(node -> requiredNode(request.parentId()).flatMap(parent -> {
            checkVersion(node, request.expectedVersion());
            if (parent.space() != node.space() || parent.type() != DriveNodeType.FOLDER || parent.id().equals(node.id()))
                return Mono.error(new ConflictException("目标父节点无效"));
            if (node.type() == DriveNodeType.FOLDER && isDescendant(parent.id(), node.id()))
                return Mono.error(new ConflictException("目录不能移动到自身后代"));
            boolean duplicate = nodes.values().stream().anyMatch(n -> n.space().equals(node.space()) && !n.id().equals(node.id())
                && java.util.Objects.equals(n.parent(), parent.id()) && n.lifecycle() == DriveLifecycle.ACTIVE
                && n.normalized().equals(node.normalized()));
            if (duplicate) return Mono.error(new ConflictException("目标目录下名称已存在"));
            Node changed = new Node(node.id(), node.space(), parent.id(), node.type(), node.name(), node.normalized(),
                node.lifecycle(), node.revision(), node.version() + 1, node.created(), Instant.now());
            nodes.put(node.id(), changed); advance(spaces.get(node.space())); return Mono.just(view(changed));
        }));
    }
    private boolean isDescendant(UUID candidate, UUID ancestor) {
        UUID current = candidate;
        while (current != null) {
            if (current.equals(ancestor)) return true;
            Node n = nodes.get(current); current = n == null ? null : n.parent();
        }
        return false;
    }
    @Override public Mono<DriveNodeView> trash(UUID actorId, UUID nodeId, long expectedVersion) { return changeLifecycle(actorId,nodeId,expectedVersion,DriveLifecycle.TRASHED); }
    @Override public Mono<DriveNodeView> restore(UUID actorId, UUID nodeId, long expectedVersion) { return changeLifecycle(actorId,nodeId,expectedVersion,DriveLifecycle.ACTIVE); }
    @Override public Mono<DriveRevisionView> createRevision(UUID actorId, UUID nodeId, CreateDriveRevisionRequest request) { return ownedNode(actorId,nodeId).flatMap(n->{ if(n.type()!=DriveNodeType.FILE)return Mono.error(new ConflictException("只有文件节点可以创建版本")); checkVersion(n,request.expectedNodeVersion()); UUID rid=ids.next(); Node changed=new Node(n.id(),n.space(),n.parent(),n.type(),n.name(),n.normalized(),n.lifecycle(),rid,n.version()+1,n.created(),Instant.now()); nodes.put(nodeId,changed); advance(spaces.get(n.space())); return Mono.just(new DriveRevisionView(rid,nodeId,n.version()+1,request.attachmentId(),request.contentFingerprint(),Instant.now(),Instant.now(),actorId)); }); }
    @Override public Flux<DriveRevisionView> revisions(UUID actorId, UUID nodeId) { return ownedNode(actorId,nodeId).flatMapMany(n -> n.revision()==null ? Flux.empty() : Flux.just(new DriveRevisionView(n.revision(),nodeId,1,UUID.randomUUID(),null,null,n.updated(),actorId))); }
    @Override public Flux<DriveChangeView> changes(UUID actorId, UUID spaceId, long afterSequence) { return ownedSpace(actorId,spaceId).flatMapMany(s -> Flux.empty()); }
    @Override public Mono<DriveQuotaView> quota(UUID actorId, UUID spaceId) { return ownedSpace(actorId,spaceId).map(s -> new DriveQuotaView(spaceId,Long.MAX_VALUE,0,0,Long.MAX_VALUE)); }
    @Override public Mono<DriveQuotaReservationView> beginUpload(UUID actorId, UUID spaceId, BeginDriveUploadRequest request) { return ownedSpace(actorId,spaceId).map(s -> new DriveQuotaReservationView(ids.next(),spaceId,request.uploadSessionId(),request.reservedBytes(),QuotaReservationState.ACTIVE,Instant.now().plusSeconds(3600))); }
    @Override public Mono<DriveQuotaReservationView> finalizeUpload(UUID actorId, UUID spaceId, UUID reservationId) { return Mono.error(new NotFoundException("配额 reservation 不存在")); }
    @Override public Mono<DriveQuotaReservationView> abortUpload(UUID actorId, UUID spaceId, UUID reservationId) { return Mono.error(new NotFoundException("配额 reservation 不存在")); }
    @Override public Mono<SyncBindingView> createBinding(UUID actorId, CreateSyncBindingRequest request) { return Mono.error(new UnsupportedOperationException("内存 Drive 未实现同步 Binding")); }
    @Override public Flux<SyncBindingView> bindings(UUID actorId) { return Flux.empty(); }
    @Override public Mono<SyncBindingView> setBindingEnabled(UUID actorId, UUID bindingId, boolean enabled) { return Mono.error(new NotFoundException("Sync Binding 不存在")); }
    @Override public Mono<SyncConflictView> createConflict(UUID actorId, CreateSyncConflictRequest request) { return Mono.error(new UnsupportedOperationException("内存 Drive 未实现 Conflict")); }
    @Override public Flux<SyncConflictView> conflicts(UUID actorId, UUID bindingId) { return Flux.empty(); }
    @Override public Mono<SyncConflictView> resolveConflict(UUID actorId, UUID conflictId, SyncConflictState state) { return Mono.error(new NotFoundException("Conflict 不存在")); }
    @Override public Mono<DeviceView> registerDevice(UUID actorId, RegisterDeviceRequest request) { return Mono.error(new UnsupportedOperationException("内存 Drive 未实现 Device")); }
    @Override public Flux<DeviceView> devices(UUID actorId) { return Flux.empty(); }
    @Override public Mono<DeviceView> revokeDevice(UUID actorId, UUID deviceId) { return Mono.error(new NotFoundException("Device 不存在")); }
    @Override public Mono<SyncMappingView> upsertMapping(UUID actorId, UUID bindingId, UpsertSyncMappingRequest request) { return Mono.error(new NotFoundException("Sync Binding 不存在")); }
    @Override public Flux<SyncMappingView> mappings(UUID actorId, UUID bindingId) { return Flux.empty(); }
    @Override public Flux<DriveTombstoneView> tombstones(UUID actorId, UUID spaceId, long afterSequence) { return ownedSpace(actorId,spaceId).flatMapMany(s->Flux.empty()); }
    @Override public Flux<SyncMutationResult> applyMutations(UUID actorId, UUID bindingId, java.util.List<SyncMutationRequest> requests) { return Flux.fromIterable(requests).concatMap(request -> { Mono<DriveNodeView> action = switch (request.kind()) { case RENAME -> rename(actorId,request.nodeId(),new RenameDriveNodeRequest(request.name(),request.expectedVersion())); case MOVE -> move(actorId,request.nodeId(),new MoveDriveNodeRequest(request.parentId(),request.expectedVersion())); case TRASH -> trash(actorId,request.nodeId(),request.expectedVersion()); case RESTORE -> restore(actorId,request.nodeId(),request.expectedVersion()); }; return action.map(node->new SyncMutationResult(request.operationId(),true,node,null,null)).onErrorResume(error->Mono.just(new SyncMutationResult(request.operationId(),false,null,error.getClass().getSimpleName(),error.getMessage()))); }); }
    @Override public Mono<SyncBindingView> advanceCursor(UUID actorId, UUID bindingId, long cursor) { return Mono.error(new NotFoundException("Sync Binding 不存在")); }
    @Override public Mono<SyncBindingView> requestFullResync(UUID actorId, UUID bindingId) { return Mono.error(new NotFoundException("Sync Binding 不存在")); }
    @Override public Mono<CameraBackupView> updateCameraBackup(UUID actorId, UUID bindingId, CameraBackupRequest request) { return Mono.error(new NotFoundException("Sync Binding 不存在")); }
    @Override public Flux<CameraBackupView> cameraBackups(UUID actorId, UUID bindingId) { return Flux.empty(); }
    private Mono<DriveNodeView> changeLifecycle(UUID actor, UUID id, long expected, DriveLifecycle target) {
        return ownedNode(actor,id).flatMap(node -> { checkVersion(node, expected); if (node.lifecycle()==DriveLifecycle.PURGED) return Mono.error(new ConflictException("已永久删除的节点不能恢复"));
            Node changed = new Node(node.id(),node.space(),node.parent(),node.type(),node.name(),node.normalized(),target,node.revision(),node.version()+1,node.created(),Instant.now()); nodes.put(id,changed); advance(spaces.get(node.space())); return Mono.just(view(changed)); });
    }
    private Mono<Space> ownedSpace(UUID actor, UUID id) { return Mono.justOrEmpty(spaces.get(id)).filter(s -> s.owner().equals(actor)).switchIfEmpty(Mono.error(new NotFoundException("Drive Space 不存在"))); }
    private Mono<Node> ownedNode(UUID actor, UUID id) { return Mono.justOrEmpty(nodes.get(id)).flatMap(n -> ownedSpace(actor,n.space()).thenReturn(n)).switchIfEmpty(Mono.error(new NotFoundException("Drive Node 不存在"))); }
    private Mono<Node> requiredNode(UUID id) { return Mono.justOrEmpty(nodes.get(id)).switchIfEmpty(Mono.error(new NotFoundException("父节点不存在"))); }
    private void checkVersion(Node n,long expected) { if (expected != n.version()) throw new ConflictException("Drive Node 版本冲突"); }
    private void advance(Space s) { if (s != null) spaces.replace(s.id(), new Space(s.id(),s.owner(),s.name(),s.root(),s.generation()+1, s.created(),Instant.now(),s.version()+1)); }
    private String normalize(String name) { return Normalizer.normalize(name.trim(), Normalizer.Form.NFKC).toLowerCase(java.util.Locale.ROOT); }
    private DriveSpaceView view(Space s) { return new DriveSpaceView(s.id(),s.owner(),s.name(),s.root(),s.generation(),s.created(),s.updated(),s.version()); }
    private DriveNodeView view(Node n) { return new DriveNodeView(n.id(),n.space(),n.parent(),n.type(),n.name(),n.normalized(),n.lifecycle(),n.revision(),n.version(),n.created(),n.updated()); }
}
