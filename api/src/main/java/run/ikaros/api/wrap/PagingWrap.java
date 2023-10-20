package run.ikaros.api.wrap;


import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import lombok.Data;
import org.springframework.data.util.Streamable;
import org.springframework.util.Assert;

@Data
public class PagingWrap<T> implements Streamable<T> {

    @Schema(description = "Page number, starts from 1. If not set or equal to 0, it means no "
        + "pagination.", requiredMode = REQUIRED)
    private final int page;

    @Schema(description = "Size of each page. If not set or equal to 0, it means no pagination.",
        requiredMode = REQUIRED)
    private final int size;

    @Schema(description = "Total elements.", requiredMode = REQUIRED)
    private final long total;

    @Schema(description = "A chunk of items.", requiredMode = REQUIRED)
    private final List<T> items;

    /**
     * new a page result instance.
     *
     * @param page  page
     * @param size  size
     * @param total total
     * @param items item list
     */
    public PagingWrap(int page, int size, long total, List<T> items) {
        Assert.isTrue(total >= 0, "Total elements must be greater than or equal to 0");
        if (page < 0) {
            page = 0;
        }
        if (size < 0) {
            size = 0;
        }
        if (items == null) {
            items = Collections.emptyList();
        }
        this.page = page;
        this.size = size;
        this.total = total;
        this.items = items;
    }

    public PagingWrap(List<T> items) {
        this(0, 0, items.size(), items);
    }

    @Schema(description = "Indicates whether current page is the first page.",
        requiredMode = REQUIRED)
    public boolean isFirstPage() {
        return !hasPrevious();
    }

    @Schema(description = "Indicates whether current page is the last page.",
        requiredMode = REQUIRED)
    public boolean isLastPage() {
        return !hasNext();
    }

    /**
     * if this has next.
     *
     * @return true if this has next
     */
    @Schema(description = "Indicates whether current page has previous page.",
        requiredMode = REQUIRED)
    @JsonProperty("hasNext")
    public boolean hasNext() {
        if (page <= 0) {
            return false;
        }
        return page < getTotalPages();
    }

    @Schema(description = "Indicates whether current page has previous page.",
        requiredMode = REQUIRED)
    @JsonProperty("hasPrevious")
    public boolean hasPrevious() {
        return page > 1;
    }

    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }

    @Override
    @JsonIgnore
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Schema(description = "Indicates total pages.", requiredMode = REQUIRED)
    @JsonProperty("totalPages")
    public long getTotalPages() {
        return size == 0 ? 1 : (total + size - 1) / size;
    }


    public static <T> PagingWrap<T> emptyResult() {
        return new PagingWrap<>(List.of());
    }

}
