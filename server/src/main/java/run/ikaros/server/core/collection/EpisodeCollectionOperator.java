package run.ikaros.server.core.collection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.collection.EpisodeCollection;
import run.ikaros.api.core.collection.EpisodeCollectionOperate;

@Slf4j
@Component
public class EpisodeCollectionOperator implements EpisodeCollectionOperate {
    private final EpisodeCollectionService service;

    public EpisodeCollectionOperator(EpisodeCollectionService service) {
        this.service = service;
    }


    @Override
    public Mono<EpisodeCollection> create(Long userId, Long episodeId) {
        return service.create(userId, episodeId);
    }

    @Override
    public Mono<EpisodeCollection> remove(Long userId, Long episodeId) {
        return service.remove(userId, episodeId);
    }

    @Override
    public Mono<EpisodeCollection> findByUserIdAndEpisodeId(Long userId, Long episodeId) {
        return service.findByUserIdAndEpisodeId(userId, episodeId);
    }

    @Override
    public Mono<Void> updateEpisodeCollectionProgress(Long userId, Long episodeId, Long progress) {
        return service.updateEpisodeCollectionProgress(userId, episodeId, progress);
    }

    @Override
    public Mono<Void> updateEpisodeCollectionFinish(Long userId, Long episodeId, Boolean finish) {
        return service.updateEpisodeCollectionFinish(userId, episodeId, finish);
    }
}
