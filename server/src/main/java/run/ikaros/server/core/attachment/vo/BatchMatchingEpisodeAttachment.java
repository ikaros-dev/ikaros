package run.ikaros.server.core.attachment.vo;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Data;
import org.jspecify.annotations.Nullable;

@Data
public class BatchMatchingEpisodeAttachment {
    @Schema(requiredMode = REQUIRED, description = "episode id for subject.")
    private @Nullable UUID episodeId;
    @Schema(requiredMode = REQUIRED, description = "attachment id array.")
    private UUID @Nullable [] attachmentIds;
}
