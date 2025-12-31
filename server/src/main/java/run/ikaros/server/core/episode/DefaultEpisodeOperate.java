package run.ikaros.server.core.episode;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.EpisodeOperate;
import run.ikaros.api.core.subject.EpisodeResource;
import run.ikaros.api.store.enums.EpisodeGroup;

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
    public Mono<Episode> findById(UUID episodeId) {
        return service.findById(episodeId);
    }

    @Override
    public Flux<Episode> findAllBySubjectId(UUID subjectId) {
        return service.findAllBySubjectId(subjectId);
    }

    @Override
    public Mono<Episode> findBySubjectIdAndGroupAndSequenceAndName(
        UUID subjectId, EpisodeGroup group, Float sequence, String name) {
        return service.findBySubjectIdAndGroupAndSequenceAndName(
            subjectId, group, sequence, name);
    }

    @Override
    public Flux<Episode> findBySubjectIdAndGroupAndSequence(UUID subjectId, EpisodeGroup group,
                                                            Float sequence) {
        return service.findBySubjectIdAndGroupAndSequence(subjectId, group, sequence);
    }

    @Override
    public Mono<Long> countBySubjectId(UUID subjectId) {
        return service.countBySubjectId(subjectId);
    }

    @Override
    public Mono<Long> countMatchingBySubjectId(UUID subjectId) {
        return service.countMatchingBySubjectId(subjectId);
    }

    @Override
    public Flux<EpisodeResource> findResourcesById(UUID episodeId) {
        return service.findResourcesById(episodeId);
    }

    @Override
    public Flux<Episode> updateEpisodesWithSubjectId(UUID subjectId, List<Episode> episodes) {
        return service.updateEpisodesWithSubjectId(subjectId, episodes);
    }
}
