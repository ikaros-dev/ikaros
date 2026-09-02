package run.ikaros.drive;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
@Table("drive_node")
public record DriveNodeEntity(@Id UUID id, @Column("drive_space_id") UUID driveSpaceId,
    @Column("parent_id") UUID parentId, @Column("node_type") DriveNodeType nodeType, String name,
    @Column("normalized_name") String normalizedName, DriveLifecycle lifecycle,
    @Column("current_revision_id") UUID currentRevisionId, @Column("created_by") UUID createdBy,
    @Column("created_at") Instant createdAt, @Column("updated_at") Instant updatedAt,
    @Column("trashed_at") Instant trashedAt, @Column("node_version") long nodeVersion, @Version Long version) {}
