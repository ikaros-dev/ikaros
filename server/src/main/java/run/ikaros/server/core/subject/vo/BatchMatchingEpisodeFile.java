package run.ikaros.server.core.subject.vo;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class BatchMatchingEpisodeFile {
    @Schema(requiredMode = REQUIRED, description = "subject id for episode belong.")
    private Long subjectId;
    @Schema(requiredMode = REQUIRED, description = "file id array.")
    private Long[] fileIds;
}
