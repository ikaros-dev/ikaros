package run.ikaros.identity;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final AuthenticationService service;

    public AuthenticationController(AuthenticationService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public Mono<AuthenticationView> register(@Valid @RequestBody RegisterRequest request) {
        return service.register(request);
    }

    @PostMapping("/login")
    public Mono<AuthenticationView> login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/refresh-token")
    public Mono<AuthenticationView> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return service.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @RequestHeader("X-Ikaros-Session-Id") UUID sessionId
    ) {
        return service.logout(actorId, sessionId).thenReturn(ResponseEntity.noContent().build());
    }
}
