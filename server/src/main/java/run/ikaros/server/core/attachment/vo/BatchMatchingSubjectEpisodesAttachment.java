package run.ikaros.server.core.attachment.vo;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Data;
import org.jspecify.annotations.Nullable;
import run.ikaros.api.store.enums.EpisodeGroup;

@Data
public class BatchMatchingSubjectEpisodesAttachment {
    @Schema(requiredMode = REQUIRED, description = "subject id for episode belong.")
    private @Nullable UUID subjectId;
    @Schema(requiredMode = REQUIRED, description = "attachment id array.")
    private UUID @Nullable [] attachmentIds;
    @Schema(requiredMode = NOT_REQUIRED, description = "subject episode group.")
    private @Nullable EpisodeGroup episodeGroup;
}
