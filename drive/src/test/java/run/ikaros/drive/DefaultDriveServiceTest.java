package run.ikaros.drive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import reactor.core.publisher.Mono;

class DefaultDriveServiceTest {
    private final DefaultDriveService service = new DefaultDriveService(UUID::randomUUID, (userId, deviceId) -> Mono.just(true));
    private final UUID user = UUID.randomUUID();

    @Test void renameKeepsIdentityAndRejectsStaleVersion() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        DriveNodeView node = service.createNode(user, space.id(), new CreateDriveNodeRequest(DriveNodeType.FILE, "readme.md", null)).block();
        DriveNodeView renamed = service.rename(user, node.id(), new RenameDriveNodeRequest("README.md", 0)).block();
        assertEquals(node.id(), renamed.id());
        assertEquals(1, renamed.nodeVersion());
        assertThrows(ConflictException.class, () -> service.rename(user, node.id(), new RenameDriveNodeRequest("x", 0)).block());
    }

    @Test void createsBackupBindingForRegisteredDeviceAndDriveSpace() {
        UUID device = UUID.randomUUID();
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Backup")).block();

        SyncBindingView binding = service.createBinding(user, new CreateSyncBindingRequest(
            device, space.id(), space.rootNodeId(), "DCIM", "相机照片", SyncSourceKind.CAMERA_ROLL,
            SyncMode.BACKUP, DeletePolicy.KEEP_REMOTE, ConflictPolicy.PRESERVE_BOTH)).block();

        assertEquals(device, binding.deviceId());
        assertEquals(space.id(), binding.driveSpaceId());
        assertEquals(space.rootNodeId(), binding.remoteRootNodeId());
        assertEquals("DCIM", binding.localScopeId());
        assertEquals(SyncMode.BACKUP, binding.mode());
        assertEquals(SyncBindingState.ACTIVE, binding.state());
    }

    @Test void rejectsOverlappingWritableBackupScopesOnSameDevice() {
        UUID device = UUID.randomUUID();
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Backup")).block();
        CreateSyncBindingRequest request = new CreateSyncBindingRequest(
            device, space.id(), space.rootNodeId(), "DCIM", null, SyncSourceKind.DIRECTORY,
            SyncMode.BACKUP, DeletePolicy.KEEP_REMOTE, ConflictPolicy.PRESERVE_BOTH);

        service.createBinding(user, request).block();

        assertThrows(ConflictException.class, () -> service.createBinding(user,
            new CreateSyncBindingRequest(device, space.id(), space.rootNodeId(), "DCIM/2026", null,
                SyncSourceKind.DIRECTORY, SyncMode.BACKUP, DeletePolicy.KEEP_REMOTE,
                ConflictPolicy.PRESERVE_BOTH)).block());
    }

    @Test void trashThenRestorePreservesNodeAndAdvancesGeneration() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        DriveNodeView node = service.createNode(user, space.id(), new CreateDriveNodeRequest(DriveNodeType.FILE, "a.txt", null)).block();
        DriveNodeView trashed = service.trash(user, node.id(), 0).block();
        DriveNodeView restored = service.restore(user, node.id(), trashed.nodeVersion()).block();
        assertEquals(node.id(), restored.id());
        assertEquals(DriveLifecycle.ACTIVE, restored.lifecycle());
        assertEquals(2, restored.nodeVersion());
    }

    @Test void folderCycleIsRejected() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        DriveNodeView a = service.createNode(user, space.id(), new CreateDriveNodeRequest(DriveNodeType.FOLDER, "a", null)).block();
        DriveNodeView b = service.createNode(user, space.id(), new CreateDriveNodeRequest(DriveNodeType.FOLDER, "b", a.id())).block();
        assertThrows(ConflictException.class, () -> service.move(user, a.id(), new MoveDriveNodeRequest(b.id(), 0)).block());
    }

    @Test void restoreRejectsStaleVersionAndActiveNodes() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        DriveNodeView node = service.createNode(user, space.id(), new CreateDriveNodeRequest(DriveNodeType.FILE, "a.txt", null)).block();
        assertThrows(ConflictException.class, () -> service.restore(user, node.id(), 0).block());

        DriveNodeView trashed = service.trash(user, node.id(), 0).block();
        assertThrows(ConflictException.class, () -> service.restore(user, node.id(), 0).block());
        assertEquals(TombstoneLifecycle.TRASHED, service.tombstones(user, space.id(), 0).collectList().block().get(0).lifecycle());
        assertEquals(1, trashed.nodeVersion());
    }

    @Test void moveAndRevisionArePublishedToChangeLog() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        DriveNodeView folder = service.createNode(user, space.id(), new CreateDriveNodeRequest(DriveNodeType.FOLDER, "docs", null)).block();
        DriveNodeView file = service.createNode(user, space.id(), new CreateDriveNodeRequest(DriveNodeType.FILE, "a.txt", null)).block();

        service.move(user, file.id(), new MoveDriveNodeRequest(folder.id(), 0)).block();
        service.createRevision(user, file.id(), new CreateDriveRevisionRequest(
            UUID.randomUUID(), 1L, "sha256:test", null)).block();

        var changes = service.changes(user, space.id(), 0).collectList().block();
        assertEquals(4, changes.size());
        assertEquals(DriveMutationKind.NODE_MOVED, changes.get(2).mutationKind());
        assertEquals(DriveMutationKind.CONTENT_REVISION_CREATED, changes.get(3).mutationKind());
    }

    @Test void revisionOperationIsIdempotent() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        DriveNodeView file = service.createNode(user, space.id(), new CreateDriveNodeRequest(DriveNodeType.FILE, "a.txt", null)).block();
        UUID attachment = UUID.randomUUID();
        CreateDriveRevisionRequest request = new CreateDriveRevisionRequest(attachment, 0L, "sha256:test", "op-1");

        DriveRevisionView first = service.createRevision(user, file.id(), request).block();
        DriveRevisionView retry = service.createRevision(user, file.id(), request).block();
        assertEquals(first.id(), retry.id());
        assertEquals(1, service.revisions(user, file.id()).count().block());
    }

    @Test void restoresHistoricalRevisionAndAdvancesNodeVersion() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        DriveNodeView file = service.createNode(user, space.id(), new CreateDriveNodeRequest(DriveNodeType.FILE, "a.txt", null)).block();
        DriveRevisionView first = service.createRevision(user, file.id(), new CreateDriveRevisionRequest(
            UUID.randomUUID(), 0L, "sha256:first", "first")).block();
        DriveRevisionView second = service.createRevision(user, file.id(), new CreateDriveRevisionRequest(
            UUID.randomUUID(), 1L, "sha256:second", "second")).block();

        DriveNodeView restored = service.restoreRevision(user, file.id(), first.revisionNo(), 2L).block();

        assertEquals(first.id(), restored.currentRevisionId());
        assertEquals(3L, restored.nodeVersion());
        var history = service.revisions(user, file.id()).collectList().block();
        assertEquals(second.id(), history.get(0).id());
        assertEquals(first.id(), history.get(1).id());
    }

    @Test void historicalRevisionRestoreRejectsStaleVersionAndUnknownRevision() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        DriveNodeView file = service.createNode(user, space.id(), new CreateDriveNodeRequest(DriveNodeType.FILE, "a.txt", null)).block();
        service.createRevision(user, file.id(), new CreateDriveRevisionRequest(UUID.randomUUID(), 0L, "sha256:first", "first")).block();

        assertThrows(ConflictException.class, () -> service.restoreRevision(user, file.id(), 1L, 0L).block());
        assertThrows(NotFoundException.class, () -> service.restoreRevision(user, file.id(), 99L, 1L).block());
    }
    @Test void revisionHistoryIsNewestFirstAndKeepsCurrentRevision() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        DriveNodeView file = service.createNode(user, space.id(), new CreateDriveNodeRequest(DriveNodeType.FILE, "a.txt", null)).block();
        DriveRevisionView first = service.createRevision(user, file.id(), new CreateDriveRevisionRequest(
            UUID.randomUUID(), 0L, "sha256:first", null)).block();
        DriveRevisionView second = service.createRevision(user, file.id(), new CreateDriveRevisionRequest(
            UUID.randomUUID(), 1L, "sha256:second", null)).block();

        var history = service.revisions(user, file.id()).collectList().block();
        assertEquals(2, history.size());
        assertEquals(second.id(), history.get(0).id());
        assertEquals(first.id(), history.get(1).id());
        assertEquals(second.id(), service.node(user, file.id()).block().currentRevisionId());

    }

    @Test void deletePropagationHonorsBindingPolicy() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        DriveNodeView node = service.createNode(user, space.id(), new CreateDriveNodeRequest(DriveNodeType.FILE, "a.txt", null)).block();
        SyncBindingView binding = service.createBinding(user, new CreateSyncBindingRequest(UUID.randomUUID(), space.id(), space.rootNodeId(),
            "scope", null, SyncSourceKind.DIRECTORY, SyncMode.TWO_WAY, DeletePolicy.KEEP_REMOTE, ConflictPolicy.PRESERVE_BOTH)).block();

        SyncMutationResult blocked = service.applyMutations(user, binding.id(), java.util.List.of(
            new SyncMutationRequest("delete-1", node.id(), SyncMutationKind.TRASH, node.nodeVersion(), null, null))).blockFirst();

        assertEquals(false, blocked.applied());
        assertEquals("DELETE_POLICY_BLOCKED", blocked.errorCode());
        assertEquals(DriveLifecycle.ACTIVE, service.node(user, node.id()).block().lifecycle());
    }

    @Test void reconnectResumesTwoWayBindingFromPersistedCursor() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        SyncBindingView binding = service.createBinding(user, new CreateSyncBindingRequest(UUID.randomUUID(), space.id(),
            space.rootNodeId(), "documents", null, SyncSourceKind.DIRECTORY, SyncMode.TWO_WAY,
            DeletePolicy.KEEP_REMOTE, ConflictPolicy.PRESERVE_BOTH)).block();
        service.advanceCursor(user, binding.id(), 7).block();
        service.setBindingEnabled(user, binding.id(), false).block();

        SyncBindingView resumed = service.resumeSync(user, binding.id()).block();

        assertEquals(SyncBindingState.ACTIVE, resumed.state());
        assertEquals(true, resumed.enabled());
        assertEquals(7, resumed.cursor());
    }

    @Test void reconnectIsIdempotentForActiveBinding() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        SyncBindingView binding = service.createBinding(user, new CreateSyncBindingRequest(UUID.randomUUID(), space.id(),
            space.rootNodeId(), "documents", null, SyncSourceKind.DIRECTORY, SyncMode.TWO_WAY,
            DeletePolicy.KEEP_REMOTE, ConflictPolicy.PRESERVE_BOTH)).block();

        SyncBindingView resumed = service.resumeSync(user, binding.id()).block();

        assertEquals(binding.id(), resumed.id());
        assertEquals(binding.cursor(), resumed.cursor());
        assertEquals(SyncBindingState.ACTIVE, resumed.state());
    }

    @Test void uploadReservationIsIdempotentForSameSession() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        UUID uploadSession = UUID.randomUUID();
        BeginDriveUploadRequest request = new BeginDriveUploadRequest(uploadSession, 1024);

        DriveQuotaReservationView first = service.beginUpload(user, space.id(), request).block();
        DriveQuotaReservationView retry = service.beginUpload(user, space.id(), request).block();
        assertEquals(first.id(), retry.id());
        assertEquals(1024, service.quota(user, space.id()).block().reservedBytes());
    }

    @Test void uploadReservationRejectsQuotaOverflow() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        BeginDriveUploadRequest request = new BeginDriveUploadRequest(UUID.randomUUID(), 100L * 1024 * 1024 * 1024 + 1);
        assertThrows(ConflictException.class, () -> service.beginUpload(user, space.id(), request).block());
    }

    @Test void initialBackupResetsCursorAndMarksBindingForRescan() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        UUID device = UUID.randomUUID();
        SyncBindingView binding = service.createBinding(user, new CreateSyncBindingRequest(device, space.id(),
            space.rootNodeId(), "camera-roll", "/Pictures", SyncSourceKind.DIRECTORY, SyncMode.BACKUP,
            DeletePolicy.KEEP_REMOTE, ConflictPolicy.PRESERVE_BOTH)).block();

        service.advanceCursor(user, binding.id(), 42).block();
        SyncBindingView rescan = service.requestFullResync(user, binding.id()).block();

        assertEquals(SyncBindingState.DEGRADED, rescan.state());
        assertEquals(0, rescan.cursor());
        assertEquals(SyncMode.BACKUP, rescan.mode());
        assertEquals(binding.id(), rescan.id());
    }

    @Test void initialBackupRejectsNonBackupBinding() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        SyncBindingView binding = service.createBinding(user, new CreateSyncBindingRequest(UUID.randomUUID(), space.id(),
            space.rootNodeId(), "documents", null, SyncSourceKind.DIRECTORY, SyncMode.TWO_WAY,
            DeletePolicy.KEEP_REMOTE, ConflictPolicy.PRESERVE_BOTH)).block();

        assertThrows(ConflictException.class, () -> service.requestFullResync(user, binding.id()).block());
    }

    @Test void cameraBackupDetectionUpsertsChangedSourceItem() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        UUID deviceId = UUID.randomUUID();
        SyncBindingView binding = service.createBinding(user, new CreateSyncBindingRequest(deviceId, space.id(),
            space.rootNodeId(), "camera-roll", "Camera Roll", SyncSourceKind.CAMERA_ROLL, SyncMode.BACKUP,
            DeletePolicy.KEEP_REMOTE, ConflictPolicy.PRESERVE_BOTH)).block();

        CameraBackupView discovered = service.updateCameraBackup(user, binding.id(), new CameraBackupRequest(
            " photo-1 ", CameraBackupState.DISCOVERED, null, null, "sha256:old", null)).block();
        CameraBackupView changed = service.updateCameraBackup(user, binding.id(), new CameraBackupRequest(
            "photo-1", CameraBackupState.QUEUED, null, null, "sha256:new", null)).block();

        assertEquals(discovered.id(), changed.id());
        assertEquals("photo-1", changed.sourceItemId());
        assertEquals(CameraBackupState.QUEUED, changed.state());
        assertEquals("sha256:new", changed.contentFingerprint());
        assertEquals(1, service.cameraBackups(user, binding.id()).count().block());
        assertEquals(0, service.cameraBackups(user, binding.id(), true).count().block());
        service.updateCameraBackup(user, binding.id(), new CameraBackupRequest("camera-2", CameraBackupState.ERROR,
            null, null, "sha256:bad", "读取源文件失败")).block();
        assertEquals(1, service.cameraBackups(user, binding.id(), true).count().block());
        assertEquals("读取源文件失败", service.cameraBackups(user, binding.id(), true).next().block().errorMessage());
    }

    @Test void cameraBackupDetectionRejectsInvalidDowngradeAfterVerification() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        SyncBindingView binding = service.createBinding(user, new CreateSyncBindingRequest(UUID.randomUUID(), space.id(),
            space.rootNodeId(), "camera-roll", "Camera Roll", SyncSourceKind.CAMERA_ROLL, SyncMode.BACKUP,
            DeletePolicy.KEEP_REMOTE, ConflictPolicy.PRESERVE_BOTH)).block();
        service.updateCameraBackup(user, binding.id(), new CameraBackupRequest("photo-2",
            CameraBackupState.BACKUP_VERIFIED, null, null, "sha256:verified", null)).block();

        assertThrows(ConflictException.class, () -> service.updateCameraBackup(user, binding.id(), new CameraBackupRequest(
            "photo-2", CameraBackupState.ERROR, null, null, "sha256:broken", "upload failed")).block());
    }

    @Test void retriesOnlyFailedCameraBackupAndClearsFailureDetails() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Backup")).block();
        SyncBindingView binding = service.createBinding(user, new CreateSyncBindingRequest(UUID.randomUUID(), space.id(),
            space.rootNodeId(), "DCIM", "Camera Roll", SyncSourceKind.CAMERA_ROLL, SyncMode.BACKUP,
            DeletePolicy.KEEP_REMOTE, ConflictPolicy.PRESERVE_BOTH)).block();
        CameraBackupView failed = service.updateCameraBackup(user, binding.id(), new CameraBackupRequest("photo-1",
            CameraBackupState.ERROR, UUID.randomUUID(), UUID.randomUUID(), "sha256:photo-1", "读取原图失败")).block();

        CameraBackupView retried = service.retryCameraBackup(user, binding.id(), failed.id()).block();

        assertEquals(failed.id(), retried.id());
        assertEquals(CameraBackupState.QUEUED, retried.state());
        assertEquals("sha256:photo-1", retried.contentFingerprint());
        assertEquals(null, retried.errorMessage());
        assertThrows(ConflictException.class, () -> service.retryCameraBackup(user, binding.id(), retried.id()).block());
    }

    @Test void cameraBackupDetectionDeduplicatesConcurrentSameFingerprint() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        SyncBindingView binding = service.createBinding(user, new CreateSyncBindingRequest(UUID.randomUUID(), space.id(),
            space.rootNodeId(), "camera-roll", "Camera Roll", SyncSourceKind.CAMERA_ROLL, SyncMode.BACKUP,
            DeletePolicy.KEEP_REMOTE, ConflictPolicy.PRESERVE_BOTH)).block();
        CameraBackupRequest request = new CameraBackupRequest(" photo-duplicate ", CameraBackupState.QUEUED,
            null, null, " sha256:same ", null);

        var results = reactor.core.publisher.Flux.range(0, 8)
            .flatMap(ignored -> service.updateCameraBackup(user, binding.id(), request), 8)
            .collectList().block();

        assertEquals(8, results.size());
        assertEquals(1, service.cameraBackups(user, binding.id()).count().block());
        assertEquals(7, results.stream().filter(CameraBackupView::deduplicated).count());
        assertEquals(1, results.stream().filter(result -> !result.deduplicated()).count());
        assertEquals("sha256:same", results.get(0).contentFingerprint());
        assertEquals("CONTENT_FINGERPRINT_ALREADY_BACKED_UP", results.get(1).deduplicationReason());
    }

    @Test void cameraBackupScanTreatsChangedFingerprintAsNewContent() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        SyncBindingView binding = service.createBinding(user, new CreateSyncBindingRequest(UUID.randomUUID(), space.id(),
            space.rootNodeId(), "camera-roll", "Camera Roll", SyncSourceKind.CAMERA_ROLL, SyncMode.BACKUP,
            DeletePolicy.KEEP_REMOTE, ConflictPolicy.PRESERVE_BOTH)).block();
        CameraBackupScanItem first = new CameraBackupScanItem("photo-scan", "sha256:first");

        CameraBackupScanView created = service.scanCameraBackups(user, binding.id(), new CameraBackupScanRequest(java.util.List.of(first))).block();
        CameraBackupScanView unchanged = service.scanCameraBackups(user, binding.id(), new CameraBackupScanRequest(java.util.List.of(first))).block();
        CameraBackupScanView changed = service.scanCameraBackups(user, binding.id(), new CameraBackupScanRequest(
            java.util.List.of(new CameraBackupScanItem(" photo-scan ", " sha256:second ")))).block();

        assertEquals(1, created.discovered().size());
        assertEquals(1, unchanged.known().size());
        assertEquals(true, unchanged.known().get(0).deduplicated());
        assertEquals(1, changed.discovered().size());
        assertEquals("sha256:second", changed.discovered().get(0).contentFingerprint());
        assertEquals("CONTENT_FINGERPRINT_CHANGED", changed.discovered().get(0).deduplicationReason());
    }

    @Test void cameraBackupScanReturnsOnlyPreviouslyUnseenPhotosAsDiscovered() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        SyncBindingView binding = service.createBinding(user, new CreateSyncBindingRequest(UUID.randomUUID(), space.id(),
            space.rootNodeId(), "camera-roll", "Camera Roll", SyncSourceKind.CAMERA_ROLL, SyncMode.BACKUP,
            DeletePolicy.KEEP_REMOTE, ConflictPolicy.PRESERVE_BOTH)).block();
        service.updateCameraBackup(user, binding.id(), new CameraBackupRequest("known-photo", CameraBackupState.BACKUP_VERIFIED,
            null, null, "sha256:known", null)).block();

        CameraBackupScanView result = service.scanCameraBackups(user, binding.id(), new CameraBackupScanRequest(java.util.List.of(
            new CameraBackupScanItem("known-photo", "sha256:known"),
            new CameraBackupScanItem("new-photo", "sha256:new")))).block();

        assertEquals(1, result.discovered().size());
        assertEquals("new-photo", result.discovered().getFirst().sourceItemId());
        assertEquals(1, result.known().size());
        assertEquals(CameraBackupState.BACKUP_VERIFIED, result.known().getFirst().state());
        assertEquals(2, service.cameraBackups(user, binding.id()).count().block());
    }

    @Test void cameraBackupScanRejectsNonBackupBinding() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        SyncBindingView binding = service.createBinding(user, new CreateSyncBindingRequest(UUID.randomUUID(), space.id(),
            space.rootNodeId(), "documents", null, SyncSourceKind.DIRECTORY, SyncMode.TWO_WAY,
            DeletePolicy.KEEP_REMOTE, ConflictPolicy.PRESERVE_BOTH)).block();

        assertThrows(NotFoundException.class, () -> service.scanCameraBackups(user, binding.id(),
            new CameraBackupScanRequest(java.util.List.of(new CameraBackupScanItem("photo", "sha256:x")))).block());
    }

    @Test void resumeInterruptedBackupKeepsCursorAndActivatesBinding() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        SyncBindingView binding = service.createBinding(user, new CreateSyncBindingRequest(UUID.randomUUID(), space.id(),
            space.rootNodeId(), "camera-roll", "Camera Roll", SyncSourceKind.DIRECTORY, SyncMode.BACKUP,
            DeletePolicy.KEEP_REMOTE, ConflictPolicy.PRESERVE_BOTH)).block();
        service.advanceCursor(user, binding.id(), 42).block();
        service.requestFullResync(user, binding.id()).block();

        SyncBindingView resumed = service.resumeBackup(user, binding.id()).block();

        assertEquals(SyncBindingState.ACTIVE, resumed.state());
        assertEquals(0, resumed.cursor());
        assertEquals(binding.id(), resumed.id());
        assertEquals(true, resumed.enabled());
    }

    @Test void resumeInterruptedBackupRejectsActiveBinding() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Personal")).block();
        SyncBindingView binding = service.createBinding(user, new CreateSyncBindingRequest(UUID.randomUUID(), space.id(),
            space.rootNodeId(), "documents", null, SyncSourceKind.DIRECTORY, SyncMode.BACKUP,
            DeletePolicy.KEEP_REMOTE, ConflictPolicy.PRESERVE_BOTH)).block();

        assertThrows(ConflictException.class, () -> service.resumeBackup(user, binding.id()).block());
    }

    @Test void preservesConflictCopyWithBothRevisionReferences() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Sync")).block();
        SyncBindingView binding = service.createBinding(user, new CreateSyncBindingRequest(UUID.randomUUID(), space.id(),
            space.rootNodeId(), "documents", null, SyncSourceKind.DIRECTORY, SyncMode.TWO_WAY,
            DeletePolicy.KEEP_REMOTE, ConflictPolicy.PRESERVE_BOTH)).block();
        DriveNodeView node = service.createNode(user, space.id(), new CreateDriveNodeRequest(DriveNodeType.FILE, "note.txt", null)).block();
        UUID base = UUID.randomUUID();
        UUID remote = UUID.randomUUID();

        SyncConflictView conflict = service.createConflict(user, new CreateSyncConflictRequest(
            binding.id(), node.id(), base, remote, "sha256:local")).block();

        assertEquals(SyncConflictState.OPEN, conflict.state());
        assertEquals(base, conflict.baseRevisionId());
        assertEquals(remote, conflict.remoteRevisionId());
        assertEquals("sha256:local", conflict.localFingerprint());
        assertEquals(conflict.id(), service.conflicts(user, binding.id()).next().block().id());

        SyncConflictView resolved = service.resolveConflict(user, conflict.id(), SyncConflictState.RESOLVED).block();
        assertEquals(SyncConflictState.RESOLVED, resolved.state());
        assertEquals(user, resolved.resolvedBy());
    }

    @Test void rejectsConflictNodeOutsideBindingSpace() {
        DriveSpaceView syncSpace = service.createSpace(user, new CreateDriveSpaceRequest("Sync")).block();
        DriveSpaceView otherSpace = service.createSpace(user, new CreateDriveSpaceRequest("Other")).block();
        SyncBindingView binding = service.createBinding(user, new CreateSyncBindingRequest(UUID.randomUUID(), syncSpace.id(),
            syncSpace.rootNodeId(), "documents", null, SyncSourceKind.DIRECTORY, SyncMode.TWO_WAY,
            DeletePolicy.KEEP_REMOTE, ConflictPolicy.PRESERVE_BOTH)).block();
        DriveNodeView foreignNode = service.createNode(user, otherSpace.id(), new CreateDriveNodeRequest(DriveNodeType.FILE, "foreign.txt", null)).block();

        assertThrows(ConflictException.class, () -> service.createConflict(user,
            new CreateSyncConflictRequest(binding.id(), foreignNode.id(), null, null, "sha256:foreign")).block());
    }
}
