package run.ikaros.server.core.service;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
     *     <li>create file hard link for original dir</li>
     * </ol>
     */
    void searchDownloadProcessAndCreateFileHardLinksAndRelateEpisode();

    /**
     * 查询用户订阅的动漫的剧集资源，如果剧集资源
     */
    void downloadSubscribeAnimeResource(@Nullable Long userId);

    /**
     * 扫描导入目录，导入新的文件
     * 具体请看：<a href="https://github.com/ikaros-dev/ikaros/issues/221">[Feature] 导入已有的番剧 #221</a>
     * <ol>
     *     <li>用户启动时，添加一个媒体库映射目录，在容器里对应的目录则是应用目录下的 import 目录</li>
     *     <li>程序会定时扫描这个目录，先将这个目录下的目录移动到 original 目录，
     *     然后将对应的番剧剧集文件硬链接到文件管理目录(upload)，再将对应录入完的目录移动回去</li>
     *     <li>用户在后台添加动漫</li>
     *     <li>用户在对应的动漫详情页面关联资源</li>
     * </ol>
     */
    void scanImportDir2ImportNewFile();
}
