package run.ikaros.resource;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
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
import run.ikaros.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * 提供 Resource 统一资源库的 HTTP-first 接口。
 */
@Validated
@RestController
@RequestMapping("/api/resources")
public class ResourceController {
    private final ResourceService resourceService;

    /**
     * 创建 Resource 控制器。
     *
     * @param resourceService Resource 业务服务
     */
    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * 创建资源及其首个主标题。
     *
     * @param actorId 当前登录用户标识
     * @param request 包含类型、标题与语言的创建请求
     * @return 新建资源的完整视图
     */
    @Operation(summary = "创建统一资源", description = "以当前用户为拥有者创建 Resource，并同时保存首个主标题。"
        + "Resource 仅表示逻辑内容身份，不会直接绑定文件路径或存储位置。")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "资源创建成功"),
        @ApiResponse(responseCode = "400", description = "用户标识或请求字段不合法", content = @Content),
        @ApiResponse(responseCode = "401", description = "调用方未提供认证主体", content = @Content)
    })
    @PostMapping
    public Mono<ResponseEntity<ResourceView>> create(
        @Parameter(description = "当前认证用户 UUID，由认证层注入", required = true, in = ParameterIn.HEADER)
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @Valid @RequestBody CreateResourceRequest request
    ) {
        return resourceService.create(actorId, request)
            .map(resource -> ResponseEntity.created(URI.create("/api/resources/" + resource.id())).body(resource));
    }

    /**
     * 分页查询当前用户的活动资源。
     *
     * @param actorId 当前登录用户标识
     * @param type 可选 Resource 类型
     * @param query 可选标题关键词
     * @param page 从零开始的页码
     * @param size 每页数量
     * @return 分页 Resource 视图
     */
    @Operation(summary = "浏览统一资源库", description = "按当前用户、类型和标题关键词查询活动 Resource。"
        + "已归档或位于回收站的资源不在默认列表中返回。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "资源列表查询成功"),
        @ApiResponse(responseCode = "400", description = "分页参数或用户标识不合法", content = @Content)
    })
    @GetMapping
    public Mono<PageResponse<ResourceView>> list(
        @Parameter(description = "当前认证用户 UUID，由认证层注入", required = true, in = ParameterIn.HEADER)
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @RequestParam(required = false) ResourceType type,
        @RequestParam(required = false) String query,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return resourceService.list(actorId, type, query, page, size);
    }

    /**
     * 读取一个当前用户可访问的资源。
     *
     * @param actorId 当前登录用户标识
     * @param resourceId Resource 标识
     * @return 完整 Resource 视图
     */
    @Operation(summary = "获取资源详情", description = "返回 Resource 的生命周期、多语言标题和外部身份映射。"
        + "若资源不属于当前用户，统一返回不存在以避免泄露对象信息。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "资源读取成功"),
        @ApiResponse(responseCode = "404", description = "资源不存在或无权访问", content = @Content)
    })
    @GetMapping("/{resourceId}")
    public Mono<ResourceView> get(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID resourceId
    ) {
        return resourceService.get(actorId, resourceId);
    }

    /**
     * 将资源移入回收站。
     *
     * @param actorId 当前登录用户标识
     * @param resourceId Resource 标识
     * @return 无响应体的完成信号
     */
    @Operation(summary = "将资源移入回收站", description = "执行逻辑删除并记录审计事件。"
        + "该操作不直接删除 Attachment、Blob 或其任一物理副本。")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "资源已移入回收站"),
        @ApiResponse(responseCode = "404", description = "资源不存在或无权访问", content = @Content)
    })
    @DeleteMapping("/{resourceId}")
    public Mono<ResponseEntity<Void>> trash(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID resourceId
    ) {
        return resourceService.trash(actorId, resourceId).thenReturn(ResponseEntity.noContent().build());
    }

    @Operation(summary = "归档资源", description = "通过显式生命周期命令归档活动 Resource，不删除任何 Attachment 或 Blob。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "资源已归档"),
        @ApiResponse(responseCode = "404", description = "资源不存在或无权访问", content = @Content),
        @ApiResponse(responseCode = "409", description = "资源当前状态不允许归档", content = @Content)
    })
    @PostMapping("/{resourceId}/archive")
    public Mono<ResourceView> archive(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID resourceId
    ) {
        return resourceService.archive(actorId, resourceId);
    }

    /**
     * 从回收站恢复资源。
     *
     * @param actorId 当前登录用户标识
     * @param resourceId Resource 标识
     * @return 已恢复的 Resource 视图
     */
    @Operation(summary = "恢复资源", description = "将当前用户回收站中的 Resource 恢复为活动状态。"
        + "恢复 Resource 不承诺其 Attachment 对应的 Blob 已可立即读取。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "资源恢复成功"),
        @ApiResponse(responseCode = "404", description = "资源不存在或无权访问", content = @Content)
    })
    @PostMapping("/{resourceId}/restore")
    public Mono<ResourceView> restore(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID resourceId
    ) {
        return resourceService.restore(actorId, resourceId);
    }

    /**
     * 添加一个外部身份映射。
     *
     * @param actorId 当前登录用户标识
     * @param resourceId Resource 标识
     * @param request Provider、类型与外部 ID
     * @return 新建映射
     */
    @Operation(summary = "绑定外部身份", description = "将外部 Provider 的身份映射绑定到 Resource。"
        + "外部 ID 不会成为 Ikaros 内部 Resource 身份，且同一外部身份只能绑定一个资源。")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "外部身份绑定成功"),
        @ApiResponse(responseCode = "404", description = "资源不存在或无权访问", content = @Content),
        @ApiResponse(responseCode = "409", description = "外部身份已绑定到其他资源", content = @Content)
    })
    @PostMapping("/{resourceId}/external-identities")
    public Mono<ResponseEntity<ExternalIdentityView>> addExternalIdentity(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID resourceId,
        @Valid @RequestBody CreateExternalIdentityRequest request
    ) {
        return resourceService.addExternalIdentity(actorId, resourceId, request)
            .map(identity -> ResponseEntity.status(201).body(identity));
    }
}
