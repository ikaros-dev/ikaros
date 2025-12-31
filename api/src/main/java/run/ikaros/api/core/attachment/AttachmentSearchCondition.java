package run.ikaros.api.core.attachment;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import run.ikaros.api.store.enums.AttachmentType;

@Data
@Builder
public class AttachmentSearchCondition {
    @Schema(requiredMode = REQUIRED, description = "第几页，从1开始, 默认为1.")
    private Integer page;
    @Schema(requiredMode = REQUIRED, description = "每页条数，默认为10.")
    private Integer size;
    @Schema(description = "父附件的ID，可为空，为空则代表附件在逻辑根目录下。")
    private UUID parentId;
    @Schema(description = "经过Basic64编码的附件名称，附件名称字段模糊查询。")
    private String name;
    @Schema(implementation = AttachmentType.class, description = "附件类型。")
    private AttachmentType type;
    @Schema(implementation = Boolean.class, description = "查询前是否刷新。")
    private Boolean refresh;
}
