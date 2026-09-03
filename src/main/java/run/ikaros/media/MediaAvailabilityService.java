package run.ikaros.media;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface MediaAvailabilityService {
    Mono<MediaAvailabilityView> get(UUID ownerId, UUID resourceId);
}
