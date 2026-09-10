package run.ikaros.music;

import java.util.UUID;
import reactor.core.publisher.Flux;

public interface MusicLyricsService {
    Flux<MusicLyricsView> list(UUID ownerId, UUID trackId);
}
