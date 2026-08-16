package run.ikaros.api.search;

import java.util.List;
import lombok.Data;
import org.jspecify.annotations.Nullable;

@Data
public class SearchResult<T> {
    private @Nullable List<T> hits;
    private @Nullable String keyword;
    private @Nullable Long total;
    private int limit;
    private long processingTimeMillis;
}
