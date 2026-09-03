package run.ikaros.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 提供 Resource 收藏状态的 HTTP-first 接口。
 */
@RestController
@RequestMapping({"/api/resources/{resourceId}/favorite"})
public class FavoriteController {
    private final FavoriteService favoriteService;

    /**
     * 创建收藏控制器。
     *
     * @param favoriteService 收藏业务服务
     */
    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /**
     * 收藏 Resource。
     *
     * @param actorId 当前认证用户标识
     * @param resourceId Resource 标识
     * @return 收藏状态
     */
    @Operation(summary = "收藏资源", description = "为当前用户建立 Resource 收藏关系；重复调用保持幂等。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "收藏成功"),
        @ApiResponse(responseCode = "404", description = "资源不存在或无权访问", content = @Content)
    })
    @PostMapping
    public Mono<FavoriteView> add(
        @Parameter(description = "当前认证用户 UUID，由认证层注入", required = true, in = ParameterIn.HEADER)
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID resourceId
    ) {
        return favoriteService.add(actorId, resourceId);
    }

    /**
     * 取消收藏 Resource。
     *
     * @param actorId 当前认证用户标识
     * @param resourceId Resource 标识
     * @return 空响应
     */
    @Operation(summary = "取消收藏资源", description = "删除当前用户的 Resource 收藏关系；重复调用保持幂等。")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "取消收藏成功"),
        @ApiResponse(responseCode = "404", description = "资源不存在或无权访问", content = @Content)
    })
    @DeleteMapping
    public Mono<ResponseEntity<Void>> remove(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID resourceId
    ) {
        return favoriteService.remove(actorId, resourceId).thenReturn(ResponseEntity.noContent().build());
    }

    /**
     * 查询当前用户的 Resource 收藏状态。
     *
     * @param actorId 当前认证用户标识
     * @param resourceId Resource 标识
     * @return 收藏状态
     */
    @Operation(summary = "查询资源收藏状态", description = "返回当前用户是否已经收藏指定 Resource。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "收藏状态查询成功"),
        @ApiResponse(responseCode = "404", description = "资源不存在或无权访问", content = @Content)
    })
    @GetMapping
    public Mono<FavoriteView> get(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID resourceId
    ) {
        return favoriteService.get(actorId, resourceId);
    }
}
