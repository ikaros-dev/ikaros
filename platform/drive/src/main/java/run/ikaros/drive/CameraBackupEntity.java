package run.ikaros.drive;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
@Table("drive_camera_backup_mapping")
public record CameraBackupEntity(@Id UUID id, @Column("binding_id") UUID bindingId,
    @Column("source_item_id") String sourceItemId, CameraBackupState state,
    @Column("remote_node_id") UUID remoteNodeId, @Column("remote_revision_id") UUID remoteRevisionId,
    @Column("content_fingerprint") String contentFingerprint, @Column("error_message") String errorMessage,
    @Column("updated_at") Instant updatedAt, @Version Long version) {}
