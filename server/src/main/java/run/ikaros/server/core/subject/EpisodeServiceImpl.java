package run.ikaros.server.core.subject;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.EpisodeFileEntity;
import run.ikaros.server.store.repository.EpisodeFileRepository;

@Slf4j
@Service
public class EpisodeServiceImpl implements EpisodeFileService {
    private final EpisodeFileRepository episodeFileRepository;

    public EpisodeServiceImpl(EpisodeFileRepository episodeFileRepository) {
        this.episodeFileRepository = episodeFileRepository;
    }

    @Override
    public Mono<Void> create(@Nonnull Long episodeId, @Nonnull Long fileId) {
        Assert.isTrue(episodeId > 0, "'episodeId' must gt 0.");
        Assert.isTrue(fileId > 0, "'episodeId' must gt 0.");

        return episodeFileRepository.existsByEpisodeIdAndFileId(episodeId, fileId)
            .filter(exists -> !exists)
            .flatMap(exists -> episodeFileRepository.save(EpisodeFileEntity.builder()
                .episodeId(episodeId).fileId(fileId)
                .build()))
            .then();
    }

    @Override
    public Mono<Void> remove(@NotNull Long episodeId, @NotNull Long fileId) {
        Assert.isTrue(episodeId > 0, "'episodeId' must gt 0.");
        Assert.isTrue(fileId > 0, "'episodeId' must gt 0.");
        return episodeFileRepository.existsByEpisodeIdAndFileId(episodeId, fileId)
            .filter(exists -> exists)
            .flatMap(exists -> episodeFileRepository.deleteByEpisodeIdAndFileId(episodeId, fileId))
            .then();
    }
}
