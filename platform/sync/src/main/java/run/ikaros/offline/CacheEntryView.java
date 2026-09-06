package run.ikaros.offline;
import java.time.Instant;
import java.util.UUID;
public record CacheEntryView(UUID id, UUID userId, UUID deviceId, UUID resourceId, UUID attachmentId,
    long sizeBytes, String contentFingerprint, CacheEntryState state, Instant lastAccessedAt,
    Instant createdAt, Instant updatedAt) {}
