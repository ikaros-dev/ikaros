package run.ikaros.search;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SearchPage(List<SearchResult> items, String nextCursor) {
    public record SearchResult(UUID resourceId, long sourceVersion, Map<String, Object> fields) { }
}
