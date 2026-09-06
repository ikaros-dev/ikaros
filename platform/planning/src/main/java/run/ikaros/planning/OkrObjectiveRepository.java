package run.ikaros.planning; import java.util.UUID; import org.springframework.data.repository.reactive.ReactiveCrudRepository; import reactor.core.publisher.Flux;
public interface OkrObjectiveRepository extends ReactiveCrudRepository<OkrObjectiveEntity,UUID>{Flux<OkrObjectiveEntity> findAllByOwnerIdAndCycleIdOrderByCreatedAtDesc(UUID ownerId,UUID cycleId);}
