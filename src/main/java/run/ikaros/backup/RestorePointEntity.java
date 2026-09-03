package run.ikaros.backup;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
@Table("backup_restore_point")
public record RestorePointEntity(@Id UUID id, @Column("format_version") String formatVersion,
    @Column("source_instance_id") String sourceInstanceId, @Column("schema_version") String schemaVersion,
    @Column("manifest_digest") String manifestDigest, RestorePointState state,
    @Column("verification_level") VerificationLevel verificationLevel,
    @Column("verification_status") VerificationStatus verificationStatus,
    @Column("failure_reason") String failureReason, @Column("checked_objects") long checkedObjects,
    @Column("failed_objects") long failedObjects, @Column("created_at") Instant createdAt,
    @Column("published_at") Instant publishedAt, @Version Long version) {}
