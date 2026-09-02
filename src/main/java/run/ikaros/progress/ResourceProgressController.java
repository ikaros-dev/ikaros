package run.ikaros.progress;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 提供 Resource 统一消费进度的 HTTP-first 接口。
 */
@RestController
@RequestMapping("/api/resources/{resourceId}/progress")
public class ResourceProgressController {
    private final ResourceProgressService progressService;

    /** 创建消费进度控制器。 */
    public ResourceProgressController(ResourceProgressService progressService) {
        this.progressService = progressService;
    }

    /** 设置 Resource 消费进度。 */
    @Operation(summary = "设置资源消费进度", description = "按视频秒数、音频秒数、阅读页码或百分比保存当前用户进度。"
        + "同一用户、Resource 和进度类型重复提交会更新原记录。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "进度保存成功"),
        @ApiResponse(responseCode = "400", description = "进度参数不合法", content = @Content),
        @ApiResponse(responseCode = "404", description = "资源不存在或无权访问", content = @Content)
    })
    @PutMapping
    public Mono<ResourceProgressView> set(
        @Parameter(description = "当前认证用户 UUID，由认证层注入", required = true, in = ParameterIn.HEADER)
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID resourceId,
        @Valid @RequestBody SetProgressRequest request
    ) {
        return progressService.set(actorId, resourceId, request);
    }

    /** 查询 Resource 消费进度。 */
    @Operation(summary = "查询资源消费进度", description = "返回当前用户指定类型的消费进度。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "进度查询成功"),
        @ApiResponse(responseCode = "404", description = "资源或进度不存在", content = @Content)
    })
    @GetMapping
    public Mono<ResourceProgressView> get(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID resourceId,
        @RequestParam ProgressType type
    ) {
        return progressService.get(actorId, resourceId, type);
    }
}
