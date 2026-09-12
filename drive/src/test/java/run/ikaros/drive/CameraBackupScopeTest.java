package run.ikaros.drive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;

class CameraBackupScopeTest {
    private final DefaultDriveService service = new DefaultDriveService(UUID::randomUUID, (userId, deviceId) -> Mono.just(true));
    private final UUID user = UUID.randomUUID();

    @Test void configuresAllPhotosScopeAndPersistsItOnBinding() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Photos")).block();
        SyncBindingView binding = service.createBinding(user, new CreateSyncBindingRequest(UUID.randomUUID(), space.id(),
            space.rootNodeId(), "camera-roll", "Camera Roll", SyncSourceKind.CAMERA_ROLL, SyncMode.BACKUP,
            DeletePolicy.KEEP_REMOTE, ConflictPolicy.PRESERVE_BOTH)).block();

        CameraBackupScopeView result = service.configureCameraBackupScope(user, binding.id(),
            new CameraBackupScopeRequest(CameraBackupScopeKind.ALL_PHOTOS, null)).block();

        assertEquals(CameraBackupScopeKind.ALL_PHOTOS, result.scopeKind());
        assertEquals("camera-roll:all-photos", result.localScopeId());
        assertEquals("camera-roll:all-photos", service.bindings(user).next().block().localScopeId());
    }

    @Test void configuresAlbumScopeAndRejectsInvalidOrUnauthorizedTarget() {
        DriveSpaceView space = service.createSpace(user, new CreateDriveSpaceRequest("Photos")).block();
        SyncBindingView binding = service.createBinding(user, new CreateSyncBindingRequest(UUID.randomUUID(), space.id(),
            space.rootNodeId(), "camera-roll", null, SyncSourceKind.CAMERA_ROLL, SyncMode.BACKUP,
            DeletePolicy.KEEP_REMOTE, ConflictPolicy.PRESERVE_BOTH)).block();

        CameraBackupScopeView result = service.configureCameraBackupScope(user, binding.id(),
            new CameraBackupScopeRequest(CameraBackupScopeKind.ALBUM, " album-2026 ")).block();

        assertEquals("album-2026", result.albumId());
        assertEquals("camera-roll:album:album-2026", result.localScopeId());
        assertThrows(ConflictException.class, () -> service.configureCameraBackupScope(user, binding.id(),
            new CameraBackupScopeRequest(CameraBackupScopeKind.ALBUM, " ")).block());
        assertThrows(Exception.class, () -> service.configureCameraBackupScope(UUID.randomUUID(), binding.id(),
            new CameraBackupScopeRequest(CameraBackupScopeKind.ALL_PHOTOS, null)).block());
    }
}
