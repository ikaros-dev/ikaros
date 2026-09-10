package run.ikaros.drive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class SyncMutationServiceTest {
    private final DefaultDriveService service = new DefaultDriveService(UUID::randomUUID, (userId, deviceId) -> Mono.just(true));
    private final UUID actor = UUID.randomUUID();

    @Test
    void appliesDeviceMutationsInOrderAndReturnsUpdatedNode() {
        DriveSpaceView space = service.createSpace(actor, new CreateDriveSpaceRequest("Personal")).block();
        DriveNodeView target = service.createNode(actor, space.id(), new CreateDriveNodeRequest(DriveNodeType.FOLDER, "Archive", null)).block();
        DriveNodeView file = service.createNode(actor, space.id(), new CreateDriveNodeRequest(DriveNodeType.FILE, "photo.jpg", null)).block();
        SyncBindingView binding = binding(space);

        List<SyncMutationResult> results = service.applyMutations(actor, binding.id(), List.of(
            new SyncMutationRequest("move-1", file.id(), SyncMutationKind.MOVE, file.nodeVersion(), null, target.id()),
            new SyncMutationRequest("rename-1", file.id(), SyncMutationKind.RENAME, file.nodeVersion() + 1, "renamed.jpg", null)
        )).collectList().block();

        assertEquals(2, results.size());
        assertEquals(true, results.get(0).applied());
        assertEquals(true, results.get(1).applied());
        assertEquals("renamed.jpg", results.get(1).node().name());
        assertEquals(target.id(), results.get(1).node().parentId());
    }

    @Test
    void returnsPerOperationConflictWithoutDroppingBatch() {
        DriveSpaceView space = service.createSpace(actor, new CreateDriveSpaceRequest("Personal")).block();
        DriveNodeView file = service.createNode(actor, space.id(), new CreateDriveNodeRequest(DriveNodeType.FILE, "photo.jpg", null)).block();
        SyncBindingView binding = binding(space);

        List<SyncMutationResult> results = service.applyMutations(actor, binding.id(), List.of(
            new SyncMutationRequest("rename-1", file.id(), SyncMutationKind.RENAME, file.nodeVersion(), "renamed.jpg", null),
            new SyncMutationRequest("stale-1", file.id(), SyncMutationKind.TRASH, file.nodeVersion(), null, null)
        )).collectList().block();

        assertEquals(true, results.get(0).applied());
        assertEquals(false, results.get(1).applied());
        assertEquals("stale-1", results.get(1).operationId());
        assertEquals("ConflictException", results.get(1).errorCode());
    }

    private SyncBindingView binding(DriveSpaceView space) {
        return service.createBinding(actor, new CreateSyncBindingRequest(
            UUID.randomUUID(), space.id(), space.rootNodeId(), "Documents", null, SyncSourceKind.DIRECTORY,
            SyncMode.TWO_WAY, DeletePolicy.PROPAGATE, ConflictPolicy.PRESERVE_BOTH)).block();
    }
}
