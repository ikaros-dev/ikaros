package run.ikaros.drive;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
@Table("drive_file_revision")
public record DriveFileRevisionEntity(@Id UUID id, @Column("file_node_id") UUID fileNodeId,
    @Column("revision_no") long revisionNo, @Column("attachment_id") UUID attachmentId,
    @Column("content_fingerprint") String contentFingerprint, @Column("content_modified_at") Instant contentModifiedAt,
    @Column("created_by") UUID createdBy, @Column("operation_id") String operationId,
    @Column("created_at") Instant createdAt, @Version Long version) {}
