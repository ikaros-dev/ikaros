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
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import run.ikaros.common.IfMatchVersion;

@RestController
@RequestMapping({"/api/resources/{resourceId}/user-state", "/api/v2/resources/{resourceId}/user-state"})
public class UserResourceStateController {
    private final UserResourceStateService service;

    public UserResourceStateController(UserResourceStateService service) { this.service = service; }

    @GetMapping
    public Mono<ResponseEntity<UserResourceStateView>> get(@RequestHeader("X-Ikaros-Actor-Id") UUID userId,
                                           @PathVariable UUID resourceId) {
        return service.get(userId, resourceId)
            .map(view -> ResponseEntity.ok().eTag(IfMatchVersion.etag(view.version())).body(view));
    }

    @PutMapping
    public Mono<ResponseEntity<UserResourceStateView>> set(@RequestHeader("X-Ikaros-Actor-Id") UUID userId,
                                           @PathVariable UUID resourceId,
                                           @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                           @Valid @RequestBody UserResourceStateRequest request) {
        return service.set(userId, resourceId, request, IfMatchVersion.parse(ifMatch))
            .map(view -> ResponseEntity.ok().eTag(IfMatchVersion.etag(view.version())).body(view));
    }
}
