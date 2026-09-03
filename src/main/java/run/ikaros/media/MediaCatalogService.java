package run.ikaros.media;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MediaCatalogService {
    Mono<MediaSubjectView> createSubject(UUID ownerId, CreateMediaSubjectRequest request);
    Flux<MediaSubjectView> listSubjects(UUID ownerId);
    Mono<MediaSeasonView> createSeason(UUID ownerId, UUID subjectId, CreateMediaSeasonRequest request);
    Flux<MediaSeasonView> listSeasons(UUID ownerId, UUID subjectId);
    Mono<MediaEpisodeView> createEpisode(UUID ownerId, UUID subjectId, UUID seasonId, CreateMediaEpisodeRequest request);
    Flux<MediaEpisodeView> listEpisodes(UUID ownerId, UUID subjectId, UUID seasonId);
}
