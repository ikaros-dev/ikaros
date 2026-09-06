package run.ikaros.drive;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
@Table("drive_sync_binding")
public record SyncBindingEntity(@Id UUID id, @Column("user_id") UUID userId, @Column("device_id") UUID deviceId,
    @Column("drive_space_id") UUID driveSpaceId, @Column("remote_root_node_id") UUID remoteRootNodeId,
    @Column("local_scope_id") String localScopeId, @Column("local_display_path") String localDisplayPath,
    SyncSourceKind sourceKind, SyncMode mode, DeletePolicy deletePolicy, ConflictPolicy conflictPolicy,
    boolean enabled, SyncBindingState state, long cursor, @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt, @Version Long version) {}
