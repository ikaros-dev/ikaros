package run.ikaros.drive;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
@Table("drive_sync_tombstone")
public record DriveTombstoneEntity(@Id UUID id, @Column("drive_space_id") UUID driveSpaceId,
    @Column("node_id") UUID nodeId, long sequence, @Column("node_version") long nodeVersion,
    TombstoneLifecycle lifecycle, @Column("previous_parent_id") UUID previousParentId,
    @Column("previous_name") String previousName, @Column("deleted_at") Instant deletedAt,
    @Column("retention_deadline") Instant retentionDeadline) {}
