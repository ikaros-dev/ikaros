package run.ikaros.planning; import java.util.UUID; import org.springframework.data.repository.reactive.ReactiveCrudRepository; import reactor.core.publisher.Flux;
public interface OkrKeyResultRepository extends ReactiveCrudRepository<OkrKeyResultEntity,UUID>{Flux<OkrKeyResultEntity> findAllByOwnerIdAndObjectiveIdOrderByCreatedAtDesc(UUID ownerId,UUID objectiveId);}
