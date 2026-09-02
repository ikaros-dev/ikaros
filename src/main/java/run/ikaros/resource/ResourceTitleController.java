package run.ikaros.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 提供 Resource 多语言标题的 HTTP-first 管理接口。
 */
@RestController
@RequestMapping({"/api/resources/{resourceId}/titles", "/api/v2/resources/{resourceId}/titles"})
public class ResourceTitleController {
    private final ResourceTitleService titleService;

    /**
     * 创建标题控制器。
     *
     * @param titleService 标题业务服务
     */
    public ResourceTitleController(ResourceTitleService titleService) {
        this.titleService = titleService;
    }

    /**
     * 新增或修改指定语言标题。
     *
     * @param actorId 当前认证用户标识
     * @param resourceId Resource 标识
     * @param request 标题语言、内容和主标题标志
     * @return 保存后的标题
     */
    @Operation(summary = "设置资源标题", description = "按语言或地区代码新增或修改 Resource 标题。"
        + "同一 Resource 的同一 locale 只有一个标题；设置主标题时会取消其他标题的主标题标志。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "标题保存成功"),
        @ApiResponse(responseCode = "400", description = "标题参数不合法", content = @Content),
        @ApiResponse(responseCode = "404", description = "资源不存在或无权访问", content = @Content)
    })
    @PutMapping
    public Mono<ResourceTitleView> set(
        @Parameter(description = "当前认证用户 UUID，由认证层注入", required = true, in = ParameterIn.HEADER)
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID resourceId,
        @Valid @RequestBody SetResourceTitleRequest request
    ) {
        return titleService.set(actorId, resourceId, request);
    }

    /**
     * 删除 Resource 的一个标题。
     *
     * @param actorId 当前认证用户标识
     * @param resourceId Resource 标识
     * @param titleId 标题标识
     * @return 空响应
     */
    @Operation(summary = "删除资源标题", description = "删除指定标题；删除主标题时自动提升同一 Resource 的其他标题。"
        + "不允许删除 Resource 的最后一个标题。")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "标题删除成功"),
        @ApiResponse(responseCode = "404", description = "资源或标题不存在", content = @Content),
        @ApiResponse(responseCode = "409", description = "不能删除最后一个标题", content = @Content)
    })
    @DeleteMapping("/{titleId}")
    public Mono<ResponseEntity<Void>> delete(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID resourceId,
        @PathVariable UUID titleId
    ) {
        return titleService.delete(actorId, resourceId, titleId)
            .thenReturn(ResponseEntity.noContent().build());
    }
}
