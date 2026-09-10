package run.ikaros.drive;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.PageResponse;
public interface DriveService {
    Mono<DriveSpaceView> createSpace(UUID actorId, CreateDriveSpaceRequest request);
    Flux<DriveSpaceView> listSpaces(UUID actorId);
    Flux<DriveNodeView> children(UUID actorId, UUID spaceId, UUID parentId);
    Mono<DriveNodeView> node(UUID actorId, UUID nodeId);
    default Mono<PageResponse<DriveNodeView>> childrenPage(UUID actorId, UUID spaceId, UUID parentId, int page, int size) {
        return children(actorId, spaceId, parentId).collectList().map(all -> new PageResponse<>(all.stream().skip((long) page * size).limit(size).toList(), all.size(), page, size));
    }
    Mono<DriveNodeView> createNode(UUID actorId, UUID spaceId, CreateDriveNodeRequest request);
    Mono<DriveNodeView> rename(UUID actorId, UUID nodeId, RenameDriveNodeRequest request);
    Mono<DriveNodeView> move(UUID actorId, UUID nodeId, MoveDriveNodeRequest request);
    Mono<DriveNodeView> trash(UUID actorId, UUID nodeId, long expectedVersion);
    Mono<DriveNodeView> restore(UUID actorId, UUID nodeId, long expectedVersion);
    Mono<DriveRevisionView> createRevision(UUID actorId, UUID nodeId, CreateDriveRevisionRequest request);
    Flux<DriveRevisionView> revisions(UUID actorId, UUID nodeId);
    Flux<DriveChangeView> changes(UUID actorId, UUID spaceId, long afterSequence);
    Mono<DriveQuotaView> quota(UUID actorId, UUID spaceId);
    Mono<DriveQuotaReservationView> beginUpload(UUID actorId, UUID spaceId, BeginDriveUploadRequest request);
    Mono<DriveQuotaReservationView> finalizeUpload(UUID actorId, UUID spaceId, UUID reservationId);
    Mono<DriveQuotaReservationView> abortUpload(UUID actorId, UUID spaceId, UUID reservationId);
    Mono<SyncBindingView> createBinding(UUID actorId, CreateSyncBindingRequest request);
    Flux<SyncBindingView> bindings(UUID actorId);
    Mono<SyncBindingView> setBindingEnabled(UUID actorId, UUID bindingId, boolean enabled);
    Mono<SyncConflictView> createConflict(UUID actorId, CreateSyncConflictRequest request);
    Flux<SyncConflictView> conflicts(UUID actorId, UUID bindingId);
    Mono<SyncConflictView> resolveConflict(UUID actorId, UUID conflictId, SyncConflictState state);
    Mono<SyncMappingView> upsertMapping(UUID actorId, UUID bindingId, UpsertSyncMappingRequest request);
    Flux<SyncMappingView> mappings(UUID actorId, UUID bindingId);
    Flux<DriveTombstoneView> tombstones(UUID actorId, UUID spaceId, long afterSequence);
    Flux<SyncMutationResult> applyMutations(UUID actorId, UUID bindingId, java.util.List<SyncMutationRequest> requests);
    Mono<SyncBindingView> advanceCursor(UUID actorId, UUID bindingId, long cursor);
    Mono<SyncBindingView> requestFullResync(UUID actorId, UUID bindingId);
    Mono<CameraBackupView> updateCameraBackup(UUID actorId, UUID bindingId, CameraBackupRequest request);
    Flux<CameraBackupView> cameraBackups(UUID actorId, UUID bindingId);
}
