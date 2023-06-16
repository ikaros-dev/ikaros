package run.ikaros.server.core.subject;

import jakarta.annotation.Nonnull;
import reactor.core.publisher.Mono;

public interface EpisodeFileService {
    Mono<Void> create(@Nonnull Long episodeId, @Nonnull Long fileId);

    Mono<Void> remove(@Nonnull Long episodeId, @Nonnull Long fileId);
}
