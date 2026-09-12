package run.ikaros.document;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/documents/{documentId}/publication")
public class DocumentPublicationController {
    private final DocumentService service;

    public DocumentPublicationController(DocumentService service) {
        this.service = service;
    }

    @GetMapping
    public Mono<DocumentPublicationEntity> get(
            @RequestHeader("X-Ikaros-Actor-Id") UUID owner,
            @PathVariable UUID documentId) {
        return service.publication(owner, documentId);
    }
}
