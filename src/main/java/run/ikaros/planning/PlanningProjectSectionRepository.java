package run.ikaros.planning; import java.util.UUID; import org.springframework.data.repository.reactive.ReactiveCrudRepository; import reactor.core.publisher.Flux;
public interface PlanningProjectSectionRepository extends ReactiveCrudRepository<PlanningProjectSectionEntity,UUID>{Flux<PlanningProjectSectionEntity> findAllByProjectIdOrderByPositionAsc(UUID projectId);}
