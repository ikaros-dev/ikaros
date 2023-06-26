package run.ikaros.server.core.file;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import run.ikaros.api.store.enums.FileType;

@Data
@Builder
public class FindFileCondition {
    @Schema(requiredMode = REQUIRED, description = "第几页，从1开始, 默认为1.")
    private Integer page;
    @Schema(requiredMode = REQUIRED, description = "每页条数，默认为10.")
    private Integer size;
    @Schema(description = "经过Basic64编码的文件名称，文件名称字段模糊查询。")
    private String fileName;
    @Schema(implementation = FileType.class)
    private FileType type;
}
