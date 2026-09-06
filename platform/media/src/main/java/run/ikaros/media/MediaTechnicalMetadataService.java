package run.ikaros.media;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MediaTechnicalMetadataService {
    Mono<MediaProbeView> upsertProbe(UUID ownerId, UUID releaseId, UpsertMediaProbeRequest request);
    Mono<MediaProbeView> getProbe(UUID ownerId, UUID releaseId, String profileVersion);
    Flux<MediaExternalSubtitleView> listSubtitles(UUID ownerId, UUID releaseId);
    Mono<MediaExternalSubtitleView> addSubtitle(UUID ownerId, UUID releaseId, AddExternalSubtitleRequest request);
}
