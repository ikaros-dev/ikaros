package run.ikaros.relation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** 提供当前用户资源关系的创建、查询与删除接口。 */
@RestController
@RequestMapping("/api/resources/{resourceId}/relations")
public class ResourceRelationController {
    private final ResourceRelationService relationService;

    /**
     * 创建资源关系控制器。
     *
     * @param relationService Resource 关系服务
     */
    public ResourceRelationController(ResourceRelationService relationService) {
        this.relationService = relationService;
    }

    /**
     * 建立一条具类型和方向的资源关系。
     *
     * @param ownerId 当前用户标识
     * @param resourceId 来源资源标识
     * @param request 目标资源、关系类型和顺序
     * @return 新建关系视图
     */
    @Operation(summary = "建立资源关系", description = "在当前用户拥有的两个 Resource 之间建立明确的有向关系。"
        + "关系类型表达包含、从属、前传、改编、版本、衍生或相关等业务语义。")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "资源关系创建成功"),
        @ApiResponse(responseCode = "404", description = "资源不存在或无权访问", content = @Content),
        @ApiResponse(responseCode = "409", description = "资源自关联或重复关系", content = @Content)})
    @PostMapping
    public Mono<ResponseEntity<ResourceRelationView>> create(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
                                                               @PathVariable UUID resourceId,
                                                               @Valid @RequestBody CreateResourceRelationRequest request) {
        return relationService.create(ownerId, resourceId, request)
            .map(view -> ResponseEntity.status(201).body(view));
    }

    /**
     * 查询资源的出向关系。
     *
     * @param ownerId 当前用户标识
     * @param resourceId 来源资源标识
     * @return 关系视图流
     */
    @Operation(summary = "查看资源关系", description = "返回当前用户资源的有向出边，按关系类型和位置排序。")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "资源关系查询成功"),
        @ApiResponse(responseCode = "404", description = "资源不存在或无权访问", content = @Content)})
    @GetMapping
    public Flux<ResourceRelationView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
                                            @PathVariable UUID resourceId) {
        return relationService.list(ownerId, resourceId);
    }

    /**
     * 删除一条资源关系。
     *
     * @param ownerId 当前用户标识
     * @param resourceId 来源资源标识
     * @param relationId 关系标识
     * @return 无响应体的完成信号
     */
    @Operation(summary = "删除资源关系", description = "删除当前用户来源资源上的一条出向关系，不会删除两端 Resource。")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "资源关系已删除"),
        @ApiResponse(responseCode = "404", description = "资源或关系不存在，或无权访问", content = @Content)})
    @DeleteMapping("/{relationId}")
    public Mono<ResponseEntity<Void>> remove(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
                                              @PathVariable UUID resourceId, @PathVariable UUID relationId) {
        return relationService.remove(ownerId, resourceId, relationId).thenReturn(ResponseEntity.noContent().build());
    }
}
