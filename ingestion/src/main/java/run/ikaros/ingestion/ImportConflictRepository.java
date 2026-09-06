package run.ikaros.ingestion;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
public interface ImportConflictRepository extends ReactiveCrudRepository<ImportConflictEntity, UUID> {
    Flux<ImportConflictEntity> findAllByOwnerIdAndStatusOrderByCreatedAtAsc(UUID ownerId, String status);
    Mono<ImportConflictEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}
