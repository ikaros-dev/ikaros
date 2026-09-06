package run.ikaros.ingestion;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
public interface ImportRunRepository extends ReactiveCrudRepository<ImportRunEntity, UUID> {
    Flux<ImportRunEntity> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    Mono<ImportRunEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}
