package run.ikaros.collection;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 提供 Collection 组织能力的 HTTP-first 接口。
 */
@Validated
@RestController
@RequestMapping("/api/collections")
public class CollectionController {
    private final CollectionService collectionService;

    /**
     * 创建 Collection 控制器。
     *
     * @param collectionService Collection 业务服务
     */
    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    /**
     * 创建逻辑 Collection。
     *
     * @param actorId 当前登录用户标识
     * @param request 集合名称与描述
     * @return 已创建的集合
     */
    @Operation(summary = "创建资源集合", description = "创建当前用户拥有的逻辑 Collection。"
        + "Collection 仅组织 Resource，不对应本地文件系统目录，也不改变 Attachment 的物理位置。")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "集合创建成功"),
        @ApiResponse(responseCode = "400", description = "用户标识或请求字段不合法", content = @Content)
    })
    @PostMapping
    public Mono<ResponseEntity<CollectionView>> create(
        @Parameter(description = "当前认证用户 UUID，由认证层注入", required = true, in = ParameterIn.HEADER)
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @Valid @RequestBody CreateCollectionRequest request
    ) {
        return collectionService.create(actorId, request)
            .map(collection -> ResponseEntity.created(URI.create("/api/collections/" + collection.id()))
                .body(collection));
    }

    /**
     * 查询当前用户拥有的 Collection。
     *
     * @param actorId 当前登录用户标识
     * @return Collection 列表
     */
    @Operation(summary = "查询资源集合", description = "返回当前用户拥有的 Collection，按最近更新时间倒序排列。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "集合列表查询成功"),
        @ApiResponse(responseCode = "400", description = "用户标识不合法", content = @Content)
    })
    @GetMapping
    public Mono<List<CollectionView>> list(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId) {
        return collectionService.list(actorId);
    }

    /**
     * 向 Collection 添加一个 Resource。
     *
     * @param actorId 当前登录用户标识
     * @param collectionId Collection 标识
     * @param resourceId Resource 标识
     * @param position 集合中的排序位置
     * @return 无响应体的完成信号
     */
    @Operation(summary = "向集合添加资源", description = "只有当前用户同时拥有 Collection 与 Resource 时才允许建立成员关系。"
        + "该操作只写入逻辑关系，不复制文件也不改变 Blob Placement。")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "资源已加入集合"),
        @ApiResponse(responseCode = "404", description = "集合或资源不存在，或当前用户无权访问", content = @Content),
        @ApiResponse(responseCode = "409", description = "资源已经属于该集合", content = @Content)
    })
    @PostMapping("/{collectionId}/resources/{resourceId}")
    public Mono<ResponseEntity<Void>> addResource(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID collectionId,
        @PathVariable UUID resourceId,
        @RequestParam(defaultValue = "0") @Min(0) int position
    ) {
        return collectionService.addResource(actorId, collectionId, resourceId, position)
            .thenReturn(ResponseEntity.noContent().build());
    }

    @Operation(summary = "移动资源集合", description = "移动集合到新的父集合；系统会拒绝自引用和任意深度循环。")
    @PostMapping("/{collectionId}/move")
    public Mono<CollectionView> move(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID collectionId,
        @RequestParam(required = false) UUID parentId
    ) {
        return collectionService.move(actorId, collectionId, parentId);
    }

    @DeleteMapping("/{collectionId}/resources/{resourceId}")
    public Mono<ResponseEntity<Void>> removeResource(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID collectionId,
        @PathVariable UUID resourceId
    ) {
        return collectionService.removeResource(actorId, collectionId, resourceId)
            .thenReturn(ResponseEntity.noContent().build());
    }
}
