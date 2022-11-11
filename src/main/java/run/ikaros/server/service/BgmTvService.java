package run.ikaros.server.service;

import javax.annotation.Nonnull;
import org.springframework.retry.annotation.Retryable;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.model.bgmtv.BgmTvSubject;
import run.ikaros.server.model.dto.AnimeDTO;
import run.ikaros.server.rss.mikan.model.MikanRssItem;

/**
 * @author li-guohao
 * @link <a href="https://github.com/bangumi/api">bangumi/api</a>
 */
public interface BgmTvService {


    @Nonnull
    @Retryable
    FileEntity downloadCover(@Nonnull String url);

    @Nonnull
    AnimeDTO reqBgmtvSubject(@Nonnull Long subjectId);

    /**
     * 方法不可用
     *
     * @param queryStr 查询字符串
     * @return BgmTv Subject
     */
    @Deprecated
    BgmTvSubject findSubjectByQueryStr(@Nonnull String queryStr);

    /**
     * 解析标题，查询BgmTv, 匹配对应的条目
     * <br/>
     * title example:
     * [SweetSub&LoliHouse] 手工少女!! / Do It Yourself!!
     * - 06 [WebRip 1080p HEVC-10bit AAC][简繁日内封字幕]
     *
     * @param title title
     * @return subjectId
     */
    @Nonnull
    Long pullMetadataByTitle(@Nonnull String title);
}
