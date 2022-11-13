package run.ikaros.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import run.ikaros.server.service.TaskService;

/**
 * @author li-guohao
 */
@Configuration
@EnableScheduling
public class ScheduledTaskConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledTaskConfig.class);
    private final TaskService taskService;

    public ScheduledTaskConfig(TaskService taskService) {
        this.taskService = taskService;
    }

    @Scheduled(cron = "0 */30 * * * ?")
    public void halfHourOnceTask() {
        //taskService.pullAnimeSubscribeAndSaveMetadataAndDownloadTorrents();
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void fiveMinuteOnceTask() {
        //taskService.searchDownloadProcessAndCreateFileHardLinksAndRelateEpisode();
    }

}
