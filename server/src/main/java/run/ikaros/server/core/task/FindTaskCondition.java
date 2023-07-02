package run.ikaros.server.core.task;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import run.ikaros.api.store.enums.TaskStatus;

@Data
@Builder
public class FindTaskCondition {
    @Schema(requiredMode = REQUIRED, description = "第几页，从1开始, 默认为1.")
    private Integer page;
    @Schema(requiredMode = REQUIRED, description = "每页条数，默认为10.")
    private Integer size;
    @Schema(description = "经过Basic64编码的任务名称，模糊匹配.")
    private String name;
    @Schema(implementation = TaskStatus.class, description = "任务状态，精准匹配.")
    private TaskStatus status;
}
