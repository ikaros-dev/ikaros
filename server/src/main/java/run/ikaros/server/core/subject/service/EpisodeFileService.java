package run.ikaros.server.core.subject.service;

import jakarta.annotation.Nonnull;
import reactor.core.publisher.Mono;

public interface EpisodeFileService {
    Mono<Void> create(@Nonnull Long episodeId, @Nonnull Long fileId);

    Mono<Void> remove(@Nonnull Long episodeId, @Nonnull Long fileId);

    Mono<Void> batchMatching(@Nonnull Long subjectId, @Nonnull Long[] fileIds);
}
