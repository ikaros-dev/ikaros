package run.ikaros.server.core.attachment.vo;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import org.jspecify.annotations.Nullable;
import run.ikaros.api.store.enums.AttachmentRelationType;

@Data
public class PostAttachmentRelationsParam {
    @Schema(requiredMode = REQUIRED, description = "Master attachment id.")
    private @Nullable UUID masterId;
    @Schema(requiredMode = REQUIRED, description = "Type of attachment.")
    private @Nullable AttachmentRelationType type;
    @Schema(requiredMode = REQUIRED, description = "Related attachment ids.")
    private @Nullable List<UUID> relationIds;
}
