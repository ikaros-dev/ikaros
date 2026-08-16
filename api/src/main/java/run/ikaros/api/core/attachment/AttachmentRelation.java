package run.ikaros.api.core.attachment;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;
import run.ikaros.api.store.enums.AttachmentRelationType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AttachmentRelation {
    private @Nullable UUID id;
    @JsonProperty("attachment_id")
    private @Nullable UUID attachmentId;
    private @Nullable AttachmentRelationType type;
    @JsonProperty("relation_attachment_id")
    private @Nullable UUID relationAttachmentId;
}
