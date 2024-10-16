package run.ikaros.server.core.episode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.EpisodeOperate;
import run.ikaros.api.core.subject.EpisodeResource;

@Slf4j
@Component
public class DefaultEpisodeOperate implements EpisodeOperate {
    private final EpisodeService service;

    public DefaultEpisodeOperate(EpisodeService service) {
        this.service = service;
    }

    @Override
    public Mono<Episode> save(Episode episode) {
        return service.save(episode);
    }

    @Override
    public Mono<Episode> findById(Long episodeId) {
        return service.findById(episodeId);
    }

    @Override
    public Flux<Episode> findAllBySubjectId(Long subjectId) {
        return service.findAllBySubjectId(subjectId);
    }

    @Override
    public Mono<Long> countBySubjectId(Long subjectId) {
        return service.countBySubjectId(subjectId);
    }

    @Override
    public Mono<Long> countMatchingBySubjectId(Long subjectId) {
        return service.countMatchingBySubjectId(subjectId);
    }

    @Override
    public Flux<EpisodeResource> findResourcesById(Long episodeId) {
        return service.findResourcesById(episodeId);
    }
}
