package run.ikaros.drive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.common.ConflictException;

class DefaultDriveServiceTest {
    private final DefaultDriveService service = new DefaultDriveService();
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
}
