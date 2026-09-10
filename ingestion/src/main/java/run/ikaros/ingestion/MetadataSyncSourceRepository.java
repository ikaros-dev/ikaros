package run.ikaros.ingestion;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MetadataSyncSourceRepository
    extends ReactiveCrudRepository<MetadataSyncSourceEntity, UUID> {
    Flux<MetadataSyncSourceEntity> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);
    Mono<MetadataSyncSourceEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}
