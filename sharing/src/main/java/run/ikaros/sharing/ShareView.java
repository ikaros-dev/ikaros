package run.ikaros.sharing;

import java.time.Instant;
import java.util.UUID;

public record ShareView(
    UUID id, UUID issuerId, String targetType, UUID targetId,
    ShareGranteeType granteeType, UUID granteeId, String capabilities,
    Instant expiresAt, ShareStatus status, Instant createdAt, String token,
    ShareRestrictionView restrictions) {

  public ShareView(UUID id, UUID issuerId, String targetType, UUID targetId,
                   ShareGranteeType granteeType, UUID granteeId, String capabilities,
                   Instant expiresAt, ShareStatus status, Instant createdAt, String token) {
    this(id, issuerId, targetType, targetId, granteeType, granteeId, capabilities,
        expiresAt, status, createdAt, token, null);
  }
}
