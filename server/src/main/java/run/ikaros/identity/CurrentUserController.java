package run.ikaros.identity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** 当前认证主体的身份查询接口。 */
@RestController
@RequestMapping({"/api/me"})
public class CurrentUserController {
    private final UserService userService;

    public CurrentUserController(UserService userService) {
        this.userService = userService;
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

}
