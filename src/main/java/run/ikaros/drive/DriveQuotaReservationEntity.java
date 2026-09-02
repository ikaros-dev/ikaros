package run.ikaros.drive;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
@Table("drive_quota_reservation")
public record DriveQuotaReservationEntity(@Id UUID id, @Column("drive_space_id") UUID driveSpaceId,
    @Column("upload_session_id") UUID uploadSessionId, @Column("reserved_bytes") long reservedBytes,
    QuotaReservationState state, @Column("expires_at") Instant expiresAt,
    @Column("created_at") Instant createdAt, @Column("updated_at") Instant updatedAt, @Version Long version) {}
