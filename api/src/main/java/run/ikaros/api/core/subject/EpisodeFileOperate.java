package run.ikaros.api.core.subject;

import jakarta.annotation.Nonnull;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;

public interface EpisodeFileOperate extends AllowPluginOperate {

    Mono<Void> create(@Nonnull Long episodeId, @Nonnull Long fileId);

    Mono<Void> remove(@Nonnull Long episodeId, @Nonnull Long fileId);

    Mono<Void> batchMatching(@Nonnull Long subjectId, @Nonnull Long[] fileIds,
                             boolean notify);
}
