package run.ikaros.api.core.attachment;

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
    private Long attachmentId;
    private Long referenceId;
}
