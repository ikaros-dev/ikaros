package run.ikaros.server.core.episode.sequence;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.episode.EpisodeSequenceRegular;
import run.ikaros.api.core.episode.EpisodeSequenceRegularResult;
import run.ikaros.api.wrap.PagingWrap;

public interface EpisodeSequenceRegularService {

    Mono<EpisodeSequenceRegular> save(EpisodeSequenceRegular regular);

    Mono<Void> removeById(java.util.UUID id);

    Mono<EpisodeSequenceRegular> findById(java.util.UUID id);

    Flux<EpisodeSequenceRegular> findAll();

    Mono<PagingWrap<EpisodeSequenceRegular>> findAll(Integer page, Integer size);

    Mono<EpisodeSequenceRegularResult> match(String attachmentName);
}
