package run.ikaros.api.core.attachment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class VideoSubtitle {
    @JsonProperty("master_attachment_id")
    private Long masterAttachmentId;
    @JsonProperty("attachment_id")
    private Long attachmentId;
    private String name;
    private String url;
}
