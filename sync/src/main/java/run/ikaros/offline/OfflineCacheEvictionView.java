package run.ikaros.offline;

import java.util.UUID;

public record OfflineCacheEvictionView(UUID deviceId, int evictedCount, long evictedBytes,
    int protectedDownloadCount) {}
