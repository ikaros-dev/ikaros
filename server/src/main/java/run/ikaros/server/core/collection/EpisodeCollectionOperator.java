package run.ikaros.server.core.collection;

import java.util.UUID;
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
    public Mono<EpisodeCollection> create(UUID userId, UUID episodeId) {
        return service.create(userId, episodeId);
    }

    @Override
    public Mono<EpisodeCollection> findByUserIdAndEpisodeId(UUID userId, UUID episodeId) {
        return service.findByUserIdAndEpisodeId(userId, episodeId);
    }

    @Override
    public Mono<Void> updateEpisodeCollectionProgress(UUID userId, UUID episodeId, Long progress) {
        return service.updateEpisodeCollectionProgress(userId, episodeId, progress);
    }

    @Override
    public Mono<Void> updateEpisodeCollection(UUID userId, UUID episodeId, Long progress,
                                              Long duration) {
        return service.updateEpisodeCollection(userId, episodeId, progress, duration);
    }

    @Override
    public Mono<Void> updateEpisodeCollectionFinish(UUID userId, UUID episodeId, Boolean finish) {
        return service.updateEpisodeCollectionFinish(userId, episodeId, finish);
    }
}
