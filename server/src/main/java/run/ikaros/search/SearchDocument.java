package run.ikaros.search;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** 可重建的搜索投影，不承担业务真相。 */
public record SearchDocument(UUID documentId, UUID sourceId, long sourceVersion,
                             String projectorVersion, long rebuildGeneration,
                             Map<String, Object> fields, Instant projectedAt) {
    public SearchDocument {
        fields = Map.copyOf(fields == null ? Map.of() : fields);
    }
}
