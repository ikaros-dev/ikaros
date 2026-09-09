package run.ikaros.music;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MusicImportService {
    Mono<MusicImportView> create(UUID ownerId, CreateMusicImportRequest request, String idempotencyKey);
    Flux<MusicImportView> list(UUID ownerId);
}
