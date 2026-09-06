package run.ikaros.search;

import java.time.Instant;
import java.util.UUID;

public record ProjectionFailureView(UUID id, UUID sourceId, long sourceVersion,
                                    long rebuildGeneration, String reason, Instant failedAt,
                                    Instant resolvedAt) { }
