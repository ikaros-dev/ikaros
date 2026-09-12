package run.ikaros.drive;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
class SyncConflictResolutionTest {
    private final UUID actor = UUID.randomUUID();
    private final DefaultDriveService service = new DefaultDriveService(
        UUID::randomUUID, (userId, deviceId) -> Mono.just(true));

    @Test
    void userCanResolveOrDismissAnOpenConflict() {
        DriveSpaceView space = service.createSpace(actor, new CreateDriveSpaceRequest("Sync")).block();
        SyncBindingView binding = service.createBinding(actor, new CreateSyncBindingRequest(
            UUID.randomUUID(), space.id(), space.rootNodeId(), "documents", null,
            SyncSourceKind.DIRECTORY, SyncMode.TWO_WAY, DeletePolicy.KEEP_REMOTE,
            ConflictPolicy.PRESERVE_BOTH)).block();
        DriveNodeView node = service.createNode(actor, space.id(),
            new CreateDriveNodeRequest(DriveNodeType.FILE, "note.txt", null)).block();

        SyncConflictView resolved = service.resolveConflict(actor,
            service.createConflict(actor, new CreateSyncConflictRequest(
                binding.id(), node.id(), UUID.randomUUID(), UUID.randomUUID(), "sha256:local")).block().id(),
            SyncConflictState.RESOLVED).block();

        assertEquals(SyncConflictState.RESOLVED, resolved.state());
        assertEquals(actor, resolved.resolvedBy());

        SyncConflictView dismissed = service.resolveConflict(actor,
            service.createConflict(actor, new CreateSyncConflictRequest(
                binding.id(), node.id(), null, UUID.randomUUID(), "sha256:other")).block().id(),
            SyncConflictState.DISMISSED).block();

        assertEquals(SyncConflictState.DISMISSED, dismissed.state());
        assertEquals(actor, dismissed.resolvedBy());
    }
}
