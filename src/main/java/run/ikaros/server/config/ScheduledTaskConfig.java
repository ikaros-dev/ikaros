package run.ikaros.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import run.ikaros.server.core.service.TaskService;
import run.ikaros.server.tripartite.qbittorrent.QbittorrentClient;

/**
 * @author li-guohao
 */
@Configuration
@EnableScheduling
public class ScheduledTaskConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledTaskConfig.class);
    private final TaskService taskService;
    private final QbittorrentClient qbittorrentClient;

    public ScheduledTaskConfig(TaskService taskService, QbittorrentClient qbittorrentClient) {
        this.taskService = taskService;
        this.qbittorrentClient = qbittorrentClient;
    }

    @Scheduled(cron = "0 */30 * * * ?")
    public void halfHourOnceTask() {
        taskService.pullAnimeSubscribeAndSaveMetadataAndDownloadTorrents();
        taskService.searchDownloadProcessAndCreateFileHardLinksAndRelateEpisode();
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void fiveMinuteOnceTask() {
        qbittorrentClient.tryToResumeAllMissingFilesErroredTorrents();
    }


}
