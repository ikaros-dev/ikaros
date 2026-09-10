package run.ikaros.document;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;

@Service
public class DocumentPresenceService {
    private static final Duration TTL = Duration.ofSeconds(30);
    private final DocumentRepository documents;
    private final ConcurrentMap<UUID, ConcurrentMap<String, DocumentPresenceView>> presence = new ConcurrentHashMap<>();

    public DocumentPresenceService(DocumentRepository documents) { this.documents = documents; }

    public Mono<DocumentPresenceView> heartbeat(UUID principalId, UUID documentId, String clientId) {
        return owned(principalId, documentId).then(Mono.fromSupplier(() -> {
            DocumentPresenceView view = new DocumentPresenceView(clientId, principalId, Instant.now());
            presence.computeIfAbsent(documentId, ignored -> new ConcurrentHashMap<>()).put(clientId, view);
            return view;
        }));
    }

    public Flux<DocumentPresenceView> list(UUID principalId, UUID documentId) {
        return owned(principalId, documentId).thenMany(Flux.defer(() -> {
            Instant cutoff = Instant.now().minus(TTL);
            ConcurrentMap<String, DocumentPresenceView> entries = presence.getOrDefault(documentId, new ConcurrentHashMap<>());
            entries.entrySet().removeIf(entry -> entry.getValue().lastSeenAt().isBefore(cutoff));
            return Flux.fromIterable(entries.values());
        }));
    }

    public Mono<Void> leave(UUID principalId, UUID documentId, String clientId) {
        return owned(principalId, documentId).then(Mono.fromRunnable(() -> {
            ConcurrentMap<String, DocumentPresenceView> entries = presence.get(documentId);
            if (entries != null) entries.remove(clientId);
        }));
    }

    private Mono<Void> owned(UUID principalId, UUID documentId) {
        return documents.findById(documentId).filter(document -> document.ownerId().equals(principalId))
            .switchIfEmpty(Mono.error(new NotFoundException("Document 不存在或无权访问"))).then();
    }
}
