package run.ikaros.document;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/documents/{documentId}/presence")
public class DocumentPresenceController {
    private final DocumentPresenceService service;
    public DocumentPresenceController(DocumentPresenceService service) { this.service = service; }

    @PutMapping
    public Mono<DocumentPresenceView> heartbeat(@RequestHeader("X-Ikaros-Actor-Id") UUID actor, @PathVariable UUID documentId, @Valid @RequestBody DocumentPresenceRequest request) {
        return service.heartbeat(actor, documentId, request.clientId());
    }

    @GetMapping
    public Flux<DocumentPresenceView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID actor, @PathVariable UUID documentId) {
        return service.list(actor, documentId);
    }

    @DeleteMapping("/{clientId}")
    public Mono<Void> leave(@RequestHeader("X-Ikaros-Actor-Id") UUID actor, @PathVariable UUID documentId, @PathVariable String clientId) {
        return service.leave(actor, documentId, clientId);
    }
}
