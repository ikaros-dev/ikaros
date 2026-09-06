package run.ikaros.sync;
import java.time.Instant;
import java.util.UUID;
public record DeviceView(UUID id, UUID userId, String installationId, String displayName, String platform,
    String appVersion, DeviceTrustState trustState, Instant registeredAt, Instant lastSeenAt, Instant revokedAt) {}
