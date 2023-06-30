package run.ikaros.server.core.file.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
public class FileBatchRequest {
    @Schema(requiredMode = REQUIRED, description = "文件ID数组")
    private List<Long> fileIds;
    @Schema(requiredMode = REQUIRED, description = "远端")
    private String remote;
}
