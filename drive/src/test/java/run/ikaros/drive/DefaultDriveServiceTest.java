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
}
