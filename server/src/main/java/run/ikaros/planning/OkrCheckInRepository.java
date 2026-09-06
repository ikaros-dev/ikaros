package run.ikaros.planning; import java.util.UUID; import org.springframework.data.repository.reactive.ReactiveCrudRepository; import reactor.core.publisher.Flux;
public interface OkrCheckInRepository extends ReactiveCrudRepository<OkrCheckInEntity,UUID>{Flux<OkrCheckInEntity> findAllByOwnerIdAndKeyResultIdOrderByCreatedAtDesc(UUID ownerId,UUID keyResultId);}
