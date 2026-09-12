package run.ikaros.sharing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateShareRequest(
    @NotBlank String targetType,
    @NotNull UUID targetId,
    @NotNull ShareGranteeType granteeType,
    UUID granteeId,
    @NotBlank String capabilities,
    Instant expiresAt,
    String password,
    Boolean allowDownload,
    Integer maxAccessCount) {

  public CreateShareRequest(String targetType, UUID targetId, ShareGranteeType granteeType,
                            UUID granteeId, String capabilities, Instant expiresAt) {
    this(targetType, targetId, granteeType, granteeId, capabilities, expiresAt,
        null, null, null);
  }
}
