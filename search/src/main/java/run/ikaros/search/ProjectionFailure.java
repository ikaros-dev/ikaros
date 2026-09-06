package run.ikaros.search;

import java.time.Instant;
import java.util.UUID;

/** 投影失败记录，供重试和对账使用。 */
public record ProjectionFailure(UUID sourceId, long sourceVersion, long rebuildGeneration,
                                String reason, Instant failedAt) {
}
