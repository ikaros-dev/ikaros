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
    private record Device(UUID id, UUID user, String installation, String displayName, String platform,
        String appVersion, DeviceTrustState trust, Instant registered, Instant lastSeen, Instant revoked) {}
    private record Reservation(UUID id, UUID space, UUID upload, long bytes, QuotaReservationState state,
        Instant expires) {}
    private record Binding(UUID id, UUID user, UUID device, UUID space, UUID root, String scope, String displayPath,
        SyncSourceKind sourceKind, SyncMode mode, DeletePolicy deletePolicy, ConflictPolicy conflictPolicy,
        boolean enabled, SyncBindingState state, long cursor, Instant created, Instant updated) {}
    private record Conflict(UUID id, UUID binding, UUID node, UUID baseRevision, UUID remoteRevision,
        String localFingerprint, SyncConflictState state, Instant detected, Instant resolved, UUID resolvedBy) {}
    private final UuidV7Generator ids = new UuidV7Generator();
    private final ConcurrentMap<UUID, Space> spaces = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Node> nodes = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Device> devices = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Reservation> reservations = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Binding> bindings = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Conflict> conflicts = new ConcurrentHashMap<>();

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
    @Override public Mono<DriveQuotaReservationView> beginUpload(UUID actorId, UUID spaceId, BeginDriveUploadRequest request) {
        return ownedSpace(actorId, spaceId).flatMap(s -> Mono.defer(() -> {
            Reservation existing = reservations.values().stream().filter(r -> r.space().equals(spaceId)
                && r.upload().equals(request.uploadSessionId())).findFirst().orElse(null);
            if (existing != null && existing.state() == QuotaReservationState.ACTIVE
                && existing.expires().isAfter(Instant.now())) return Mono.just(reservationView(existing));
            Instant expires = Instant.now().plusSeconds(3600);
            Reservation created = new Reservation(ids.next(), spaceId, request.uploadSessionId(), request.reservedBytes(),
                QuotaReservationState.ACTIVE, expires);
            reservations.put(created.id(), created);
            return Mono.just(reservationView(created));
        }));
    }
    @Override public Mono<DriveQuotaReservationView> finalizeUpload(UUID actorId, UUID spaceId, UUID reservationId) {
        return settleReservation(actorId, spaceId, reservationId, QuotaReservationState.COMMITTED);
    }
    @Override public Mono<DriveQuotaReservationView> abortUpload(UUID actorId, UUID spaceId, UUID reservationId) {
        return settleReservation(actorId, spaceId, reservationId, QuotaReservationState.RELEASED);
    }
    @Override public Mono<SyncBindingView> createBinding(UUID actorId, CreateSyncBindingRequest request) {
        return ownedSpace(actorId, request.driveSpaceId()).flatMap(space -> {
            Device device = devices.get(request.deviceId());
            if (device == null || !device.user().equals(actorId) || device.trust() == DeviceTrustState.REVOKED)
                return Mono.error(new ConflictException("Device 不存在或已撤销"));
            Node root = nodes.get(request.remoteRootNodeId());
            if (root == null || !root.space().equals(space.id()) || root.type() != DriveNodeType.FOLDER
                || root.lifecycle() != DriveLifecycle.ACTIVE)
                return Mono.error(new ConflictException("远端同步根目录无效"));
            String scope = normalizeScope(request.localScopeId());
            boolean write = request.mode() == SyncMode.TWO_WAY || request.mode() == SyncMode.UPLOAD_ONLY
                || request.mode() == SyncMode.BACKUP;
            boolean overlap = bindings.values().stream().anyMatch(binding -> binding.user().equals(actorId)
                && binding.device().equals(request.deviceId()) && binding.enabled() && write
                && isWrite(binding.mode()) && scopesOverlap(scope, normalizeScope(binding.scope())));
            if (overlap) return Mono.error(new ConflictException("设备本地同步 Scope 重叠"));
            Instant now = Instant.now();
            Binding binding = new Binding(ids.next(), actorId, request.deviceId(), space.id(), request.remoteRootNodeId(),
                request.localScopeId().trim(), request.localDisplayPath(), request.sourceKind(), request.mode(),
                request.deletePolicy() == null ? DeletePolicy.KEEP_REMOTE : request.deletePolicy(),
                request.conflictPolicy() == null ? ConflictPolicy.PRESERVE_BOTH : request.conflictPolicy(), true,
                SyncBindingState.ACTIVE, 0, now, now);
            bindings.put(binding.id(), binding);
            return Mono.just(bindingView(binding));
        });
    }
    @Override public Flux<SyncBindingView> bindings(UUID actorId) { return Flux.fromIterable(bindings.values())
        .filter(binding -> binding.user().equals(actorId)).sort(java.util.Comparator.comparing(Binding::created))
        .map(this::bindingView); }
    @Override public Mono<SyncBindingView> setBindingEnabled(UUID actorId, UUID bindingId, boolean enabled) {
        return Mono.justOrEmpty(bindings.get(bindingId)).filter(binding -> binding.user().equals(actorId))
            .switchIfEmpty(Mono.error(new NotFoundException("Sync Binding 不存在")))
            .map(binding -> { Binding updated = new Binding(binding.id(), binding.user(), binding.device(), binding.space(),
                binding.root(), binding.scope(), binding.displayPath(), binding.sourceKind(), binding.mode(),
                binding.deletePolicy(), binding.conflictPolicy(), enabled,
                enabled ? SyncBindingState.ACTIVE : SyncBindingState.PAUSED, binding.cursor(), binding.created(), Instant.now());
                bindings.put(bindingId, updated); return bindingView(updated); });
    }
    @Override public Mono<SyncConflictView> createConflict(UUID actorId, CreateSyncConflictRequest request) {
        return ownedBinding(actorId, request.bindingId()).flatMap(binding -> ownedNode(actorId, request.nodeId())
            .flatMap(node -> {
                if (!node.space().equals(binding.space())) return Mono.error(new ConflictException("冲突节点不属于同步空间"));
                Conflict conflict = new Conflict(ids.next(), binding.id(), node.id(), request.baseRevisionId(),
                    request.remoteRevisionId(), request.localFingerprint(), SyncConflictState.OPEN, Instant.now(), null, null);
                conflicts.put(conflict.id(), conflict);
                return Mono.just(conflictView(conflict));
            }));
    }
    @Override public Flux<SyncConflictView> conflicts(UUID actorId, UUID bindingId) {
        return ownedBinding(actorId, bindingId).flatMapMany(binding -> Flux.fromIterable(conflicts.values())
            .filter(conflict -> conflict.binding().equals(binding.id())).sort(java.util.Comparator.comparing(Conflict::detected).reversed())
            .map(this::conflictView));
    }
    @Override public Mono<SyncConflictView> resolveConflict(UUID actorId, UUID conflictId, SyncConflictState state) {
        if (state != SyncConflictState.RESOLVED && state != SyncConflictState.DISMISSED)
            return Mono.error(new ConflictException("Conflict 只能进入 RESOLVED 或 DISMISSED"));
        return Mono.justOrEmpty(conflicts.get(conflictId)).switchIfEmpty(Mono.error(new NotFoundException("Conflict 不存在")))
            .flatMap(conflict -> ownedBinding(actorId, conflict.binding()).map(binding -> {
                Conflict updated = new Conflict(conflict.id(), conflict.binding(), conflict.node(), conflict.baseRevision(),
                    conflict.remoteRevision(), conflict.localFingerprint(), state, conflict.detected(), Instant.now(), actorId);
                conflicts.put(conflictId, updated);
                return conflictView(updated);
            }));
    }
    @Override public Mono<DeviceView> registerDevice(UUID actorId, RegisterDeviceRequest request) {
        return Mono.fromSupplier(() -> {
            boolean duplicate = devices.values().stream().anyMatch(d -> d.user().equals(actorId)
                && d.installation().equals(request.installationId()) && d.revoked() == null);
            if (duplicate) throw new ConflictException("Device installation 已注册");
            Instant now = Instant.now();
            Device device = new Device(ids.next(), actorId, request.installationId().trim(), request.displayName().trim(),
                request.platform().trim(), request.appVersion(), DeviceTrustState.ACTIVE, now, now, null);
            devices.put(device.id(), device);
            return deviceView(device);
        });
    }
    @Override public Flux<DeviceView> devices(UUID actorId) { return Flux.fromIterable(devices.values())
        .filter(d -> d.user().equals(actorId) && d.revoked() == null).map(this::deviceView); }
    @Override public Mono<DeviceView> revokeDevice(UUID actorId, UUID deviceId) {
        return Mono.justOrEmpty(devices.get(deviceId)).filter(d -> d.user().equals(actorId))
            .switchIfEmpty(Mono.error(new NotFoundException("Device 不存在")))
            .map(d -> { Device revoked = new Device(d.id(), d.user(), d.installation(), d.displayName(), d.platform(),
                d.appVersion(), DeviceTrustState.REVOKED, d.registered(), d.lastSeen(), Instant.now());
                devices.put(deviceId, revoked); return deviceView(revoked); });
    }
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
    private DeviceView deviceView(Device d) { return new DeviceView(d.id(), d.user(), d.installation(), d.displayName(), d.platform(),
        d.appVersion(), d.trust(), d.registered(), d.lastSeen(), d.revoked()); }
    private Mono<DriveQuotaReservationView> settleReservation(UUID actor, UUID spaceId, UUID id,
                                                               QuotaReservationState target) {
        return ownedSpace(actor, spaceId).then(Mono.defer(() -> {
            Reservation current = reservations.get(id);
            if (current == null || !current.space().equals(spaceId)) {
                return Mono.error(new NotFoundException("配额 reservation 不存在"));
            }
            if (current.state() == QuotaReservationState.ACTIVE) {
                Reservation updated = new Reservation(current.id(), current.space(), current.upload(), current.bytes(),
                    target, current.expires());
                reservations.replace(id, current, updated);
                return Mono.just(reservationView(updated));
            }
            return Mono.just(reservationView(current));
        }));
    }
    private DriveQuotaReservationView reservationView(Reservation reservation) {
        return new DriveQuotaReservationView(reservation.id(), reservation.space(), reservation.upload(),
            reservation.bytes(), reservation.state(), reservation.expires());
    }
    private boolean isWrite(SyncMode mode) { return mode == SyncMode.TWO_WAY || mode == SyncMode.UPLOAD_ONLY
        || mode == SyncMode.BACKUP; }
    private String normalizeScope(String value) { return Normalizer.normalize(value.trim().replace('\\', '/'), Normalizer.Form.NFKC)
        .replaceAll("/+", "/").toLowerCase(java.util.Locale.ROOT); }
    private boolean scopesOverlap(String a, String b) { return a.equals(b) || a.startsWith(b.endsWith("/") ? b : b + "/")
        || b.startsWith(a.endsWith("/") ? a : a + "/"); }
    private SyncBindingView bindingView(Binding binding) { return new SyncBindingView(binding.id(), binding.user(), binding.device(),
        binding.space(), binding.root(), binding.scope(), binding.displayPath(), binding.sourceKind(), binding.mode(),
        binding.deletePolicy(), binding.conflictPolicy(), binding.enabled(), binding.state(), binding.cursor(),
        binding.created(), binding.updated()); }
    private Mono<Binding> ownedBinding(UUID actorId, UUID id) { return Mono.justOrEmpty(bindings.get(id))
        .filter(binding -> binding.user().equals(actorId))
        .switchIfEmpty(Mono.error(new NotFoundException("Sync Binding 不存在"))); }
    private SyncConflictView conflictView(Conflict conflict) { return new SyncConflictView(conflict.id(), conflict.binding(),
        conflict.node(), conflict.baseRevision(), conflict.remoteRevision(), conflict.localFingerprint(), conflict.state(),
        conflict.detected(), conflict.resolved(), conflict.resolvedBy()); }
}
