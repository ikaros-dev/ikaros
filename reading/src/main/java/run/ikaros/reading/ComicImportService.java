package run.ikaros.reading;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ComicImportService {
    Mono<ComicImportView> create(UUID ownerId, CreateComicImportRequest request, String idempotencyKey);
    Flux<ComicImportView> list(UUID ownerId);
    Mono<ComicImportView> get(UUID ownerId, UUID importId);
}
