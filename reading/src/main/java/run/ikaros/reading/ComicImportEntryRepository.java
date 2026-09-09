package run.ikaros.reading;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ComicImportEntryRepository extends ReactiveCrudRepository<ComicImportEntryEntity, UUID> {
    Flux<ComicImportEntryEntity> findAllByImportIdOrderByChapterKeyAscPageOrderAsc(UUID importId);
    reactor.core.publisher.Mono<Void> deleteAllByImportId(UUID importId);
}
