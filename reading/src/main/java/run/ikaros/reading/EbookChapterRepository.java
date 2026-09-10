package run.ikaros.reading;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EbookChapterRepository extends ReactiveCrudRepository<EbookChapterEntity, UUID> {
    Flux<EbookChapterEntity> findAllByImportIdOrderBySortOrderAsc(UUID importId);
    Mono<Void> deleteAllByImportId(UUID importId);
}
