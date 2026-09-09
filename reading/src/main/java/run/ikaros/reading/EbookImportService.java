package run.ikaros.reading;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EbookImportService {
    Mono<EbookImportView> create(UUID ownerId, CreateEbookImportRequest request, String idempotencyKey);
    Flux<EbookImportView> list(UUID ownerId);
    Mono<EbookImportView> get(UUID ownerId, UUID importId);
}
