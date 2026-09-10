package run.ikaros.reading;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EbookTocParseService {
    Mono<EbookImportView> parse(UUID ownerId, UUID importId);
    Flux<EbookChapterView> chapters(UUID ownerId, UUID importId);
}
