package run.ikaros.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 提供 Blob 多级存储 Placement 规划查询接口。
 */
@RestController
@RequestMapping({"/api/storage/blobs", "/api/v2/storage/blobs", "/api/v2/admin/blobs"})
public class StoragePlacementController {
    private final StoragePlacementService storagePlacementService;

    /**
     * 创建 Placement 控制器。
     *
     * @param storagePlacementService Placement 规划服务
     */
    public StoragePlacementController(StoragePlacementService storagePlacementService) {
        this.storagePlacementService = storagePlacementService;
    }

    @GetMapping("/{blobId}/placements")
    public reactor.core.publisher.Flux<PlacementView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID blobId) {
        return storagePlacementService.list(blobId);
    }

    /**
     * 查询 Blob 当前副本及目标层级满足情况。
     *
     * @param actorId 当前认证用户标识
     * @param blobId Blob 标识
     * @param preferredTier 优先层级
     * @param minimumReplicas 最小 ACTIVE 副本数
     * @return Placement 规划摘要
     */
    @Operation(summary = "查询 Blob 存储规划", description = "返回 Blob 的全部 Placement、ACTIVE 副本数以及目标层级是否满足。"
        + "该查询不会自动创建、迁移或删除任何副本。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "存储规划查询成功"),
        @ApiResponse(responseCode = "404", description = "Blob 不存在", content = @Content)
    })
    @GetMapping("/{blobId}/placement-plan")
    public Mono<StoragePlacementPlanView> inspect(
        @Parameter(description = "当前认证用户 UUID，由认证层注入", required = true, in = ParameterIn.HEADER)
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID blobId,
        @RequestParam(defaultValue = "HOT") StorageTier preferredTier,
        @RequestParam(defaultValue = "1") int minimumReplicas
    ) {
        return storagePlacementService.inspect(blobId, preferredTier, minimumReplicas);
    }
}
