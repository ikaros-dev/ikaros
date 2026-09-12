package run.ikaros.drive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import run.ikaros.common.api.UuidV7Generator;
import run.ikaros.sync.api.DeviceTrustQuery;

class PersistentDriveServiceTest {
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void restoresRevisionThroughPersistentRepositories() {
        DriveSpaceRepository spaces = mock(DriveSpaceRepository.class);
        DriveNodeRepository nodes = mock(DriveNodeRepository.class);
        DriveFileRevisionRepository revisions = mock(DriveFileRevisionRepository.class);
        DriveChangeRepository changes = mock(DriveChangeRepository.class);
        TransactionalOperator transactions = mock(TransactionalOperator.class);
        UUID actor = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        UUID nodeId = UUID.randomUUID();
        UUID oldRevisionId = UUID.randomUUID();
        Instant now = Instant.now();
        DriveSpaceEntity space = new DriveSpaceEntity(spaceId, actor, "Personal", UUID.randomUUID(), 4L,
            "ACTIVE", now, now, 1L);
        DriveNodeEntity node = new DriveNodeEntity(nodeId, spaceId, null, DriveNodeType.FILE, "a.txt", "a.txt",
            DriveLifecycle.ACTIVE, UUID.randomUUID(), actor, now, now, null, 2L, 1L);
        DriveFileRevisionEntity revision = new DriveFileRevisionEntity(oldRevisionId, nodeId, 1L, UUID.randomUUID(),
            "sha256:old", now, actor, "op-1", now, 1L);
        DriveNodeEntity saved = new DriveNodeEntity(node.id(), node.driveSpaceId(), node.parentId(), node.nodeType(),
            node.name(), node.normalizedName(), node.lifecycle(), oldRevisionId, node.createdBy(), node.createdAt(),
            now.plusSeconds(1), node.trashedAt(), 3L, node.version());

        when(transactions.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(nodes.findById(nodeId)).thenReturn(Mono.just(node));
        when(spaces.findById(spaceId)).thenReturn(Mono.just(space));
        when(revisions.findByFileNodeIdAndRevisionNo(nodeId, 1L)).thenReturn(Mono.just(revision));
        when(nodes.save(any(DriveNodeEntity.class))).thenReturn(Mono.just(saved));
        when(spaces.save(any(DriveSpaceEntity.class))).thenReturn(Mono.just(space));
        when(changes.save(any(DriveChangeEntity.class))).thenReturn(Mono.just(mock(DriveChangeEntity.class)));

        PersistentDriveService service = new PersistentDriveService(spaces, nodes, revisions, changes,
            mock(DriveQuotaRepository.class), mock(DriveQuotaReservationRepository.class), mock(SyncBindingRepository.class),
            mock(SyncConflictRepository.class), mock(DeviceTrustQuery.class), mock(SyncMappingRepository.class),
            mock(DriveTombstoneRepository.class), mock(CameraBackupRepository.class), transactions,
            mock(UuidV7Generator.class));

        DriveNodeView result = service.restoreRevision(actor, nodeId, 1L, 2L).block();

        assertEquals(oldRevisionId, result.currentRevisionId());
        assertEquals(3L, result.nodeVersion());
    }
}
