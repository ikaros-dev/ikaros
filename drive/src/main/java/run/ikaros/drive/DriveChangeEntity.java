package run.ikaros.drive;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
@Table("drive_change_log")
public record DriveChangeEntity(@Id UUID id, @Column("drive_space_id") UUID driveSpaceId, long sequence,
    @Column("node_id") UUID nodeId, @Column("mutation_kind") DriveMutationKind mutationKind,
    @Column("node_version") long nodeVersion, @Column("revision_id") UUID revisionId,
    @Column("occurred_at") Instant occurredAt) {}
