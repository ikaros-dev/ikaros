package run.ikaros.offline;

import java.time.Instant;
import java.util.UUID;

public record OfflineCacheQuotaView(UUID deviceId, long quotaBytes, long usedBytes,
    long availableBytes, Instant updatedAt) {}
