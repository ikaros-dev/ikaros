package run.ikaros.sharing;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateInviteRequest(@NotNull UUID inviteeId, RoomRole role, String idempotencyKey,
    Instant expiresAt) {}
