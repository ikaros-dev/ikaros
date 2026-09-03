package run.ikaros.drive;
import java.time.Instant;
import java.util.UUID;
public record DriveQuotaReservationView(UUID id, UUID spaceId, UUID uploadSessionId, long reservedBytes,
    QuotaReservationState state, Instant expiresAt) {}
