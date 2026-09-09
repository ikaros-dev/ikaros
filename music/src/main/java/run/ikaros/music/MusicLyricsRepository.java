package run.ikaros.music;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface MusicLyricsRepository extends ReactiveCrudRepository<MusicLyricsEntity, UUID> {
    Flux<MusicLyricsEntity> findAllByOwnerIdAndTrackIdOrderByCreatedAtDesc(UUID ownerId, UUID trackId);
}
