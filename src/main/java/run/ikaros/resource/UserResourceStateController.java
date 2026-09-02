package run.ikaros.resource;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/resources/{resourceId}/user-state")
public class UserResourceStateController {
    private final UserResourceStateService service;

    public UserResourceStateController(UserResourceStateService service) { this.service = service; }

    @GetMapping
    public Mono<UserResourceStateView> get(@RequestHeader("X-Ikaros-Actor-Id") UUID userId,
                                           @PathVariable UUID resourceId) {
        return service.get(userId, resourceId);
    }

    @PutMapping
    public Mono<UserResourceStateView> set(@RequestHeader("X-Ikaros-Actor-Id") UUID userId,
                                           @PathVariable UUID resourceId,
                                           @Valid @RequestBody UserResourceStateRequest request) {
        return service.set(userId, resourceId, request);
    }
}
