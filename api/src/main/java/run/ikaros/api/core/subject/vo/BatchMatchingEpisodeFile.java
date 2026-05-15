package run.ikaros.api.core.subject.vo;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Data;

@Data
public class BatchMatchingEpisodeFile {
    @Schema(requiredMode = REQUIRED, description = "subject id for episode belong.")
    private UUID subjectId;
    @Schema(requiredMode = REQUIRED, description = "file id array.")
    private UUID[] fileIds;
}
