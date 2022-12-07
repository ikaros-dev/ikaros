package run.ikaros.server.core.service;

import javax.annotation.Nullable;

/**
 * @author li-guohao
 */
public interface TaskService {

    /**
     * <ol>
     *     <li>pull anime form mikan rss subscribe</li>
     *     <li>save metadata to database</li>
     *     <li>download related torrents</li>
     * </ol>
     */
    void pullMikanRssAnimeSubscribeAndSaveMetadataAndDownloadTorrents();

    /**
     * <ol>
     *     <li>search download process has finished torrents</li>
     *     <li>create file hard link for server upload dir</li>
     *     <li>create file hard link for jellyfin media dir</li>
     * </ol>
     */
    void searchDownloadProcessAndCreateFileHardLinksAndRelateEpisode();

    /**
     * 查询用户订阅的动漫的剧集资源，如果剧集资源
     */
    void downloadSubscribeAnimeResource(@Nullable Long userId);
}
