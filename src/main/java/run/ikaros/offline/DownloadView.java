package run.ikaros.offline;
import java.time.Instant;
import java.util.UUID;
public record DownloadView(UUID id, UUID userId, UUID deviceId, UUID resourceId, UUID attachmentId,
    OfflineCopyKind kind, DownloadState state, String failureReason, long manifestVersion,
    Instant createdAt, Instant updatedAt) {}
