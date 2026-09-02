package run.ikaros.identity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** 当前认证主体的身份与会话查询接口。 */
@RestController
@RequestMapping({"/api/me", "/api/v2/me"})
public class CurrentUserController {
    private final UserService userService;
    private final SecuritySessionService sessionService;

    public CurrentUserController(UserService userService, SecuritySessionService sessionService) {
        this.userService = userService;
        this.sessionService = sessionService;
    }

    @Operation(summary = "获取当前用户", description = "根据认证层注入的 actor identity 返回当前用户资料。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "当前用户查询成功"),
        @ApiResponse(responseCode = "404", description = "当前用户不存在")
    })
    @GetMapping
    public Mono<UserView> get(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId) {
        return userService.get(actorId);
    }

    @Operation(summary = "列出当前用户会话", description = "仅返回当前用户仍有效的会话安全摘要。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "会话查询成功"),
        @ApiResponse(responseCode = "404", description = "当前用户不存在")
    })
    @GetMapping("/sessions")
    public Flux<SessionView> sessions(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId) {
        return sessionService.listActive(actorId);
    }
}
