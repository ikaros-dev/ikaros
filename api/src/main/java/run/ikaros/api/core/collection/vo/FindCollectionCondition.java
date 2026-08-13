package run.ikaros.api.core.collection.vo;

import lombok.Builder;
import lombok.Data;
import org.jspecify.annotations.Nullable;
import run.ikaros.api.store.enums.CollectionCategory;
import run.ikaros.api.store.enums.CollectionType;

@Data
@Builder
public class FindCollectionCondition {
    /**
     * default is 1.
     */
    private @Nullable Integer page;
    /**
     * default is 10.
     */
    private @Nullable Integer size;
    private @Nullable CollectionCategory category;
    @Nullable
    private CollectionType type;
    @Nullable
    private String time;
    /**
     * default is false.
     */
    private @Nullable Boolean updateTimeDesc;
}
