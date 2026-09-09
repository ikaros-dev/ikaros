package run.ikaros.search;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;

public record SearchQueryRequest(@Size(max = 200) String query,
                                  String cursor,
                                  @Max(100) Integer limit) {
    public int effectiveLimit() {
        return limit == null ? 20 : Math.max(1, Math.min(limit, 100));
    }
}
