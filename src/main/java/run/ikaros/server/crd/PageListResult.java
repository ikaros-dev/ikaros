package run.ikaros.server.crd;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.util.Streamable;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * @author: li-guohao
 */
@Data
public class PageListResult<C> implements Streamable<C> {

    @Schema(description = "Page number, starts from 1. If not set or equal to 0, it means no "
        + "pagination.", requiredMode = REQUIRED)
    private final int page;

    @Schema(description = "Size of each page. If not set or equal to 0, it means no pagination.",
        requiredMode = REQUIRED)
    private final int size;

    @Schema(description = "Total elements.", requiredMode = REQUIRED)
    private final long total;

    @Schema(description = "A chunk of items.", requiredMode = REQUIRED)
    private final List<C> items;

    public PageListResult(int page, int size, long total, List<C> items) {
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

    @Override
    public Iterator<C> iterator() {
        return items.iterator();
    }
}
