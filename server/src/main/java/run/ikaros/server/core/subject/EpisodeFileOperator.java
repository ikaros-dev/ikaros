package run.ikaros.server.core.subject;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.EpisodeFileOperate;
import run.ikaros.server.core.subject.service.EpisodeFileService;

@Slf4j
@Component
public class EpisodeFileOperator implements EpisodeFileOperate {
    private final EpisodeFileService episodeFileService;

    public EpisodeFileOperator(EpisodeFileService episodeFileService) {
        this.episodeFileService = episodeFileService;
    }

    @Override
    public Mono<Void> create(@NotNull Long episodeId, @NotNull Long fileId) {
        return episodeFileService.create(episodeId, fileId);
    }

    @Override
    public Mono<Void> remove(@NotNull Long episodeId, @NotNull Long fileId) {
        return episodeFileService.remove(episodeId, fileId);
    }

    @Override
    public Mono<Void> batchMatching(@NotNull Long subjectId, @NotNull Long[] fileIds) {
        return episodeFileService.batchMatching(subjectId, fileIds);
    }
}
