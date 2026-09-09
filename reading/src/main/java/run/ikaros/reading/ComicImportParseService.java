package run.ikaros.reading;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ComicImportParseService {
    Mono<ComicImportView> parse(UUID ownerId, UUID importId);
    Flux<ComicImportEntryEntity> entries(UUID ownerId, UUID importId);
    Mono<ComicImportView> reorder(UUID ownerId, UUID importId, ReorderComicPagesRequest request);
}
