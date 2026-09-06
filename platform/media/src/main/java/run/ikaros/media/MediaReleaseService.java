package run.ikaros.media;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MediaReleaseService {
    Mono<MediaReleaseView> add(UUID ownerId, UUID resourceId, CreateMediaReleaseRequest request);
    Flux<MediaReleaseView> list(UUID ownerId, UUID resourceId);
    Mono<MediaReleaseView> changeState(UUID ownerId, UUID releaseId, UpdateMediaReleaseStateRequest request);
}
