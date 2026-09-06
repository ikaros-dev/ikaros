package run.ikaros.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 提供 Resource 用户自定义标签的 HTTP-first 接口。
 */
@RestController
@RequestMapping({"/api/resources/{resourceId}/tags"})
public class ResourceTagController {
    private final ResourceTagService tagService;

    /** 创建标签控制器。 */
    public ResourceTagController(ResourceTagService tagService) {
        this.tagService = tagService;
    }

    /** 添加一个用户自定义标签。 */
    @Operation(summary = "为资源添加标签", description = "为当前用户拥有的 Resource 添加自定义标签；同名标签重复调用保持幂等。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "标签添加成功"),
        @ApiResponse(responseCode = "400", description = "标签参数不合法", content = @Content),
        @ApiResponse(responseCode = "404", description = "资源不存在或无权访问", content = @Content)
    })
    @PostMapping
    public Mono<ResourceTagView> add(
        @Parameter(description = "当前认证用户 UUID，由认证层注入", required = true, in = ParameterIn.HEADER)
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID resourceId,
        @Valid @RequestBody CreateResourceTagRequest request
    ) {
        return tagService.add(actorId, resourceId, request);
    }

    /** 查询资源的用户自定义标签。 */
    @Operation(summary = "查询资源标签", description = "按名称升序返回当前用户在 Resource 上设置的自定义标签。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "标签查询成功"),
        @ApiResponse(responseCode = "404", description = "资源不存在或无权访问", content = @Content)
    })
    @GetMapping
    public Mono<List<ResourceTagView>> list(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID resourceId
    ) {
        return tagService.list(actorId, resourceId);
    }

    /** 删除资源上的一个用户自定义标签。 */
    @Operation(summary = "删除资源标签", description = "删除当前用户在 Resource 上设置的指定标签。")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "标签删除成功"),
        @ApiResponse(responseCode = "404", description = "资源或标签不存在", content = @Content)
    })
    @DeleteMapping("/{tagId}")
    public Mono<ResponseEntity<Void>> remove(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID resourceId,
        @PathVariable UUID tagId
    ) {
        return tagService.remove(actorId, resourceId, tagId).thenReturn(ResponseEntity.noContent().build());
    }
}
