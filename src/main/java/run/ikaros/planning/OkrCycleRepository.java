package run.ikaros.planning; import java.util.UUID; import org.springframework.data.repository.reactive.ReactiveCrudRepository; import reactor.core.publisher.Flux;
public interface OkrCycleRepository extends ReactiveCrudRepository<OkrCycleEntity,UUID>{Flux<OkrCycleEntity> findAllByOwnerIdOrderByStartAtDesc(UUID ownerId);}
