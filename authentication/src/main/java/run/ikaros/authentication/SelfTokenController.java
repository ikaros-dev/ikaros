package run.ikaros.authentication;

import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** 当前用户的用户级 JWT 失效入口。 */
@RestController
@RequestMapping("/api/me")
public class SelfTokenController {
    private final UserService userService;

    public SelfTokenController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/actions/invalidate-tokens")
    public Mono<TokenInvalidationView> invalidateOwnTokens(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId
    ) {
        return userService.invalidateTokens(actorId, actorId);
    }
}
