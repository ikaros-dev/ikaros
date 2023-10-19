package run.ikaros.api.core.attachment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import run.ikaros.api.store.enums.AttachmentRelationType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AttachmentRelation {
    private Long id;
    @JsonProperty("attachment_id")
    private Long attachmentId;
    private AttachmentRelationType type;
    @JsonProperty("relation_attachment_id")
    private Long relationAttachmentId;
}
