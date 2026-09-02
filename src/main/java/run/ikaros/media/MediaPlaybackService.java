package run.ikaros.media;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.progress.ResourceProgressView;

public interface MediaPlaybackService {
    Mono<PlaybackSessionView> start(UUID ownerId, UUID resourceId, StartPlaybackRequest request);
    Mono<PlaybackSessionView> update(UUID ownerId, UUID sessionId, UpdatePlaybackProgressRequest request);
    Mono<PlaybackSessionView> end(UUID ownerId, UUID sessionId);
    Mono<ResourceProgressView> progress(UUID ownerId, UUID resourceId);
    Flux<PlaybackHistoryView> history(UUID ownerId);
}
