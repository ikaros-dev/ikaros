package run.ikaros.identity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 提供会话查询和撤销接口；实际登录与验证码校验由认证 Provider 后续接入。
 */
@RestController
@RequestMapping("/api/users/{userId}/sessions")
public class SecuritySessionController {
    private final SecuritySessionService sessionService;

    /**
     * 创建安全会话控制器。
     *
     * @param sessionService 安全会话服务
     */
    public SecuritySessionController(SecuritySessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * 查询用户的活跃会话。
     *
     * @param userId 用户标识
     * @return 活跃会话视图流
     */
    @Operation(summary = "查看用户活跃会话", description = "仅返回未撤销且未过期的会话安全摘要。"
        + "不会返回 Session Token、Refresh Token、IP 或完整 User-Agent 原文。")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "活跃会话查询成功")})
    @GetMapping
    public Flux<SessionView> listActive(@PathVariable UUID userId) {
        return sessionService.listActive(userId);
    }

    /**
     * 撤销一个安全会话。
     *
     * @param actorId 执行撤销的当前主体
     * @param userId 会话所属用户标识，用于清晰表达管理范围
     * @param sessionId 会话标识
     * @return 无响应体的完成信号
     */
    @Operation(summary = "撤销用户会话", description = "使指定会话立即失效并写入审计记录。"
        + "后续认证网关在下一次受保护请求时据此拒绝会话。")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "会话已撤销或原本已撤销"),
        @ApiResponse(responseCode = "404", description = "会话不存在", content = @Content)
    })
    @DeleteMapping("/{sessionId}")
    public Mono<ResponseEntity<Void>> revoke(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID userId,
        @PathVariable UUID sessionId
    ) {
        return sessionService.revoke(actorId, userId, sessionId).thenReturn(ResponseEntity.noContent().build());
    }
}
