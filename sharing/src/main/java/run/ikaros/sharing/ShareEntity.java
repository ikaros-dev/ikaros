package run.ikaros.sharing;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("share_grant")
public record ShareEntity(
    @Id UUID id,
    @Column("issuer_id") UUID issuerId,
    @Column("target_type") String targetType,
    @Column("target_id") UUID targetId,
    @Column("grantee_type") ShareGranteeType granteeType,
    @Column("grantee_id") UUID granteeId,
    String capabilities,
    @Column("token_digest") String tokenDigest,
    @Column("expires_at") Instant expiresAt,
    @Column("password_digest") String passwordDigest,
    @Column("allow_download") Boolean allowDownload,
    @Column("max_access_count") Integer maxAccessCount,
    @Column("access_count") Integer accessCount,
    ShareStatus status,
    @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt,
    @Version Long version) {

  public ShareEntity(UUID id, UUID issuerId, String targetType, UUID targetId,
                     ShareGranteeType granteeType, UUID granteeId, String capabilities,
                     String tokenDigest, Instant expiresAt, ShareStatus status,
                     Instant createdAt, Instant updatedAt, Long version) {
    this(id, issuerId, targetType, targetId, granteeType, granteeId, capabilities, tokenDigest,
        expiresAt, null, false, null, 0, status, createdAt, updatedAt, version);
  }
}
