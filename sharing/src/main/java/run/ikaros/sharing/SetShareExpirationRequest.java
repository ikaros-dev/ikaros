package run.ikaros.sharing;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record SetShareExpirationRequest(@NotNull Instant expiresAt) {}
