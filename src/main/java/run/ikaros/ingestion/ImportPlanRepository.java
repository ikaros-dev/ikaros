package run.ikaros.ingestion;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
public interface ImportPlanRepository extends ReactiveCrudRepository<ImportPlanEntity, UUID> {
    Flux<ImportPlanEntity> findAllByOwnerIdOrderByGeneratedAtDesc(UUID ownerId);
    Mono<ImportPlanEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}
