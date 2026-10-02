package run.ikaros.storage.api;

import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/** Stable paginated response for the administrative attachment listing. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AdminAttachmentPage(List<AdminAttachmentItem> items, long total, int page, int size) {
    public AdminAttachmentPage {
        items = List.copyOf(items);
    }
}
