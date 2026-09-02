package run.ikaros.planning; import java.util.UUID; import org.springframework.data.repository.reactive.ReactiveCrudRepository; import reactor.core.publisher.Flux;
public interface PlanningImportantDateRepository extends ReactiveCrudRepository<PlanningImportantDateEntity,UUID>{Flux<PlanningImportantDateEntity> findAllByOwnerIdOrderByOccursAtAsc(UUID ownerId);}
