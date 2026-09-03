package run.ikaros.ingestion;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
public interface ImportPlanItemRepository extends ReactiveCrudRepository<ImportPlanItemEntity, UUID> {
    Flux<ImportPlanItemEntity> findAllByPlanIdOrderByCreatedAtAsc(UUID planId);
}
