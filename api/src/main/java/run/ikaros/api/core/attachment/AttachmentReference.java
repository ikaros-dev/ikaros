package run.ikaros.api.core.attachment;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;
import run.ikaros.api.store.enums.AttachmentReferenceType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AttachmentReference {
    private @Nullable UUID id;
    private @Nullable AttachmentReferenceType type;
    private @Nullable UUID attachmentId;
    private @Nullable UUID referenceId;
}
