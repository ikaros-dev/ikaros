package run.ikaros.drive;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
public interface SyncMappingRepository extends ReactiveCrudRepository<SyncMappingEntity, UUID> {
    Flux<SyncMappingEntity> findAllByBindingIdOrderByUpdatedAtAsc(UUID bindingId);
    Mono<SyncMappingEntity> findByBindingIdAndLocalItemId(UUID bindingId, String localItemId);
}
