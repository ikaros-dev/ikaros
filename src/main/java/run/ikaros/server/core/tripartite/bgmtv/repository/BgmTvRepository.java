package run.ikaros.server.core.tripartite.bgmtv.repository;

import org.springframework.retry.annotation.Retryable;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvEpisode;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvEpisodeType;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvPagingData;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvSubject;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvSubjectType;
import run.ikaros.server.utils.AssertUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface BgmTvRepository {

    @Nullable
    @Retryable
    BgmTvSubject getSubject(@Nonnull Long subjectId);

    /**
     * 还无法使用
     */
    @Retryable
    @Deprecated
    BgmTvPagingData<BgmTvSubject> searchSubjectWithNextApi(@Nonnull String keyword,
                                                           @Nullable Integer offset,
                                                           @Nullable Integer limit);

    /**
     * 还无法使用
     */
    @Retryable
    @Deprecated
    default BgmTvPagingData<BgmTvSubject> searchSubjectWithNextApi(@Nonnull String keyword) {
        AssertUtils.notBlank(keyword, "keyword");
        return searchSubjectWithNextApi(keyword, null, null);
    }


    @Retryable
    List<BgmTvSubject> searchSubjectWithOldApi(@Nonnull String keyword,
                                               @Nullable BgmTvSubjectType type);

    @Retryable
    default List<BgmTvSubject> searchSubjectWithOldApi(@Nonnull String keyword) {
        AssertUtils.notBlank(keyword, "keyword");
        return searchSubjectWithOldApi(keyword, null);
    }

    @Retryable
    byte[] downloadCover(@Nonnull String url);

    @Retryable
    List<BgmTvEpisode> findEpisodesBySubjectId(@Nonnull Long subjectId,
                                               @Nonnull BgmTvEpisodeType episodeType,
                                               @Nullable Integer offset,
                                               @Nullable Integer limit);
}
