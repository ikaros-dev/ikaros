package run.ikaros.sharing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateRoomRequest(
    @NotBlank String kind,
    @NotBlank String targetType,
    @NotNull UUID targetId,
    String visibility,
    Instant expiresAt) {}
