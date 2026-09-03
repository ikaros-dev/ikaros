package run.ikaros.search;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SearchProjectionFailureRepository
    extends ReactiveCrudRepository<SearchProjectionFailureEntity, UUID> {
    Flux<SearchProjectionFailureEntity> findByResolvedAtIsNullOrderByFailedAtAsc();
    Mono<SearchProjectionFailureEntity> findByIdAndResolvedAtIsNull(UUID id);
}
