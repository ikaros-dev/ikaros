package run.ikaros.api.core.attachment;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class VideoSubtitle {
    @JsonProperty("master_attachment_id")
    private @Nullable UUID masterAttachmentId;
    @JsonProperty("attachment_id")
    private @Nullable UUID attachmentId;
    private @Nullable String name;
    private @Nullable String url;
}
