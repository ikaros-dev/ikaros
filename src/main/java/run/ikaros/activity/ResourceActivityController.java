package run.ikaros.activity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 提供用户 Activity 的记录、最近访问查询和删除接口。
 */
@Validated
@RestController
@RequestMapping("/api/activity")
public class ResourceActivityController {
    private final ResourceActivityService activityService;

    /** 创建 Activity 控制器。 */
    public ResourceActivityController(ResourceActivityService activityService) {
        this.activityService = activityService;
    }

    /** 记录一次 Resource Activity。 */
    @Operation(summary = "记录资源活动", description = "记录当前用户对 Resource 的查看、播放、阅读或下载活动。"
        + "该数据用于最近访问与历史记录，不等同于不可删除的管理审计事件。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "活动记录成功"),
        @ApiResponse(responseCode = "400", description = "活动参数不合法", content = @Content),
        @ApiResponse(responseCode = "404", description = "资源不存在或无权访问", content = @Content)
    })
    @PostMapping("/resources/{resourceId}")
    public Mono<ResourceActivityView> record(
        @Parameter(description = "当前认证用户 UUID，由认证层注入", required = true, in = ParameterIn.HEADER)
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID resourceId,
        @Valid @RequestBody RecordActivityRequest request
    ) {
        return activityService.record(actorId, resourceId, request);
    }

    /** 查询当前用户最近的 Activity。 */
    @Operation(summary = "查询最近活动", description = "按发生时间倒序返回当前用户的最近 Resource Activity。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "活动查询成功"),
        @ApiResponse(responseCode = "400", description = "数量参数不合法", content = @Content)
    })
    @GetMapping
    public Mono<List<ResourceActivityView>> recent(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit
    ) {
        return activityService.recent(actorId, limit);
    }

    /** 删除当前用户的一条可删除 Activity。 */
    @Operation(summary = "删除活动记录", description = "删除当前用户指定的 Activity，并保留删除动作本身的审计记录。")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "活动删除成功"),
        @ApiResponse(responseCode = "404", description = "活动不存在或无权访问", content = @Content)
    })
    @DeleteMapping("/{activityId}")
    public Mono<ResponseEntity<Void>> delete(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID activityId
    ) {
        return activityService.delete(actorId, activityId).thenReturn(ResponseEntity.noContent().build());
    }
}
