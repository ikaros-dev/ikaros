package run.ikaros.media;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface PlaybackSourceService {
    Mono<PlaybackSourceView> resolve(UUID ownerId, UUID resourceId, UUID preferredReleaseId);
}
