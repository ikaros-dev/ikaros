package run.ikaros.photo;

import java.util.Map;
import java.util.UUID;

public record PhotoThumbnailStatusView(UUID taskId, String status, Map<String, Object> error) {
    public PhotoThumbnailStatusView {
        error = Map.copyOf(error == null ? Map.of() : error);
    }
}
