package run.ikaros.storage.api;

import java.time.Instant;
import java.util.UUID;

public record StorageProviderProbeResult(UUID providerId, StorageProviderProbeStatus status,
                                         boolean connection, boolean read, boolean write,
                                         Instant checkedAt, String errorCode) { }
