package run.ikaros.search;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import run.ikaros.resource.api.ResourceOwnershipQuery;

@Service
public class PersistentSearchQueryService implements SearchQueryService {
    private final SearchDocumentRepository documents;
    private final SearchProjectionService projections;
    private final ResourceOwnershipQuery ownership;

    public PersistentSearchQueryService(SearchDocumentRepository documents,
                                        SearchProjectionService projections,
                                        ResourceOwnershipQuery ownership) {
        this.documents = documents;
        this.projections = projections;
        this.ownership = ownership;
    }

    @Override
    public Mono<SearchPage> search(UUID actorId, SearchQueryRequest request) {
        if (actorId == null || request == null || request.query() == null || request.query().isBlank()) {
            return Mono.just(new SearchPage(java.util.List.of(), null));
        }
        int limit = request.effectiveLimit();
        Cursor cursor = Cursor.decode(request.cursor());
        return documents.search("%" + escapeLike(request.query().trim()) + "%", cursor.at(), cursor.id(), limit * 4)
            .flatMap(entity -> ownership.requireOwned(actorId, entity.sourceId())
                .then(Mono.defer(() -> projections.get(entity.sourceId())))
                .map(document -> new Hit(entity.projectedAt(), entity.documentId(),
                    new SearchPage.SearchResult(document.sourceId(), document.sourceVersion(), document.fields())))
                .onErrorResume(error -> Mono.empty()))
            .take(limit)
            .collectList()
            .map(items -> new SearchPage(items.stream().map(Hit::result).toList(),
                items.size() == limit ? cursorFor(items.get(items.size() - 1)) : null));
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static String cursorFor(Hit hit) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
            (hit.at() + "\n" + hit.id()).getBytes(StandardCharsets.UTF_8));
    }

    private record Hit(Instant at, UUID id, SearchPage.SearchResult result) { }

    private record Cursor(Instant at, UUID id) {
        static Cursor decode(String value) {
            if (value == null || value.isBlank()) return new Cursor(Instant.MAX, new UUID(Long.MAX_VALUE, Long.MAX_VALUE));
            try {
                String[] parts = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8).split("\n");
                return new Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
            } catch (RuntimeException error) {
                return new Cursor(Instant.MAX, new UUID(Long.MAX_VALUE, Long.MAX_VALUE));
            }
        }
    }
}
