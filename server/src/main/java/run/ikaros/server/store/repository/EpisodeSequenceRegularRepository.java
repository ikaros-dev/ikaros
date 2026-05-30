package run.ikaros.server.store.repository;

import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.EpisodeSequenceRegularEntity;

public interface EpisodeSequenceRegularRepository
    extends BaseRepository<EpisodeSequenceRegularEntity> {

    Flux<EpisodeSequenceRegularEntity> findAllByEnabledTrueOrderByPriorityDesc();

    Flux<EpisodeSequenceRegularEntity> findAllByEnabledTrueOrderByPriorityDesc(Pageable pageable);

    Mono<Long> countAllByEnabledTrue();

    Mono<EpisodeSequenceRegularEntity> findByName(String name);
}
