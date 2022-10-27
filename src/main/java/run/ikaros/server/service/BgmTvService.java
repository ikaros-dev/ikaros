package run.ikaros.server.service;

import javax.annotation.Nonnull;
import org.springframework.retry.annotation.Retryable;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.model.dto.AnimeDTO;

/**
 * @author li-guohao
 */
public interface BgmTvService {


    @Nonnull
    @Retryable
    FileEntity downloadCover(@Nonnull String url);

    @Nonnull
    AnimeDTO reqBgmtvSubject(@Nonnull Long subjectId);
}
