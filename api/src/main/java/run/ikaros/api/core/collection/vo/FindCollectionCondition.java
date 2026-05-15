package run.ikaros.api.core.collection.vo;

import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import run.ikaros.api.store.enums.CollectionCategory;
import run.ikaros.api.store.enums.CollectionType;

@Data
@Builder
public class FindCollectionCondition {
    /**
     * default is 1.
     */
    private Integer page;
    /**
     * default is 10.
     */
    private Integer size;
    private CollectionCategory category;
    @Nullable
    private CollectionType type;
    @Nullable
    private String time;
    /**
     * default is false.
     */
    private Boolean updateTimeDesc;
}
