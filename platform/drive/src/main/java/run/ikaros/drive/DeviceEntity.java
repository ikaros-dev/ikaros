package run.ikaros.drive;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
@Table("drive_device")
public record DeviceEntity(@Id UUID id, @Column("user_id") UUID userId,
    @Column("installation_id") String installationId, @Column("display_name") String displayName,
    String platform, @Column("app_version") String appVersion, @Column("trust_state") DeviceTrustState trustState,
    @Column("registered_at") Instant registeredAt, @Column("last_seen_at") Instant lastSeenAt,
    @Column("revoked_at") Instant revokedAt, @Version Long version) {}
