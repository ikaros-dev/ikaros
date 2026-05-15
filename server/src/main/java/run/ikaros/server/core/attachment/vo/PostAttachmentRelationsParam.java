package run.ikaros.server.core.attachment.vo;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import run.ikaros.api.store.enums.AttachmentRelationType;

@Data
public class PostAttachmentRelationsParam {
    @Schema(requiredMode = REQUIRED, description = "Master attachment id.")
    private UUID masterId;
    @Schema(requiredMode = REQUIRED, description = "Type of attachment.")
    private AttachmentRelationType type;
    @Schema(requiredMode = REQUIRED, description = "Related attachment ids.")
    private List<UUID> relationIds;
}
