package run.ikaros.api.core.attachment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import run.ikaros.api.store.enums.AttachmentReferenceType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AttachmentReference {
    private Long id;
    private AttachmentReferenceType type;
    @JsonProperty(" attachment_id")
    private Long attachmentId;
    @JsonProperty("reference_id")
    private Long referenceId;
}
