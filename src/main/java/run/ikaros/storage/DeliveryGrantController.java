package run.ikaros.storage;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping({"/api/attachments/{attachmentId}/delivery-grants", "/api/v2/attachments/{attachmentId}/delivery-grants"})
public class DeliveryGrantController {
    private final DeliveryGrantService service;
    public DeliveryGrantController(DeliveryGrantService service) { this.service = service; }

    @PostMapping
    public Mono<DeliveryGrantView> issue(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID attachmentId, @Valid @RequestBody(required = false) DeliveryGrantRequest request) {
        return service.issue(actorId, attachmentId, request);
    }

    @PostMapping("/{grantId}/actions/revoke")
    public Mono<Void> revoke(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID grantId) { return service.revoke(actorId, grantId); }
}
