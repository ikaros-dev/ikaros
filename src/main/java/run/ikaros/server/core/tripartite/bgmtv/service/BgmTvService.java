package run.ikaros.server.core.tripartite.bgmtv.service;

import org.springframework.retry.annotation.Retryable;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.model.dto.AnimeDTO;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvSubject;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvSubjectType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.transaction.Transactional;
import java.util.List;

public interface BgmTvService {
    @Nonnull
    List<BgmTvSubject> searchSubject(@Nonnull String keyword,
                                     @Nonnull BgmTvSubjectType type);

    @Nullable
    BgmTvSubject getSubject(@Nonnull Long subjectId);


    @Nonnull
    @Transactional(rollbackOn = Exception.class)
    FileEntity downloadCover(@Nonnull String url);

    @Nullable
    @Transactional(rollbackOn = Exception.class)
    AnimeDTO reqBgmtvSubject(@Nonnull Long subjectId);


}
