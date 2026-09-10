package run.ikaros.storage.api;

import java.time.Instant;
import java.util.UUID;

public record StorageProviderStatusView(UUID providerId, String providerStatus,
                                        StorageProviderProbeResult health,
                                        Long capacityBytes, Long usedBytes,
                                        Instant checkedAt) { }
