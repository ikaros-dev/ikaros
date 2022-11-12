package run.ikaros.server.service;

/**
 * @author li-guohao
 */
public interface TaskService {

    /**
     * <ol>
     *     <li>pull anime form Subscribe</li>
     *     <li>save metadata to database</li>
     *     <li>download related torrents</li>
     * </ol>
     */
    void pullAnimeSubscribeAndSaveMetadataAndDownloadTorrents();

    /**
     * <ol>
     *     <li>search download process has finished torrents</li>
     *     <li>create file hard link for server upload dir</li>
     *     <li>create file hard link for jellyfin media dir</li>
     * </ol>
     */
    void searchDownloadProcessAndCreateFileHardLinksAndRelateEpisode();
}
