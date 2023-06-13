package run.ikaros.server.core.file;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FindFileCondition {
    @Schema(requiredMode = REQUIRED, description = "第几页，从1开始, 默认为1.")
    private Integer page;
    @Schema(requiredMode = REQUIRED, description = "每页条数，默认为10.")
    private Integer size;
    @Schema(deprecated = true)
    private String fileName;
    @Schema(deprecated = true)
    private String keywords;
    @Schema(deprecated = true)
    private String place;
    @Schema(deprecated = true)
    private String folderName;
    @Schema(deprecated = true)
    private String md5;
}
