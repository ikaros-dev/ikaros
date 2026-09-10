package run.ikaros.music;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MusicImportRepository extends ReactiveCrudRepository<MusicImportEntity, UUID> {
    Mono<MusicImportEntity> findByOwnerIdAndIdempotencyKey(UUID ownerId, String key);
    Flux<MusicImportEntity> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
}
