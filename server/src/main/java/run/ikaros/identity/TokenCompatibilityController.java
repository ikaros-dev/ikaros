package run.ikaros.identity;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** 兼容控制台当前使用的 /api/refresh-token 地址。 */
@RestController
@RequestMapping("/api")
public class TokenCompatibilityController {
    private final AuthenticationService service;

    public TokenCompatibilityController(AuthenticationService service) {
        this.service = service;
    }

    @PostMapping("/refresh-token")
    public Mono<AuthenticationView> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return service.refresh(request.refreshToken());
    }
}
