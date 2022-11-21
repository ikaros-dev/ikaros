package run.ikaros.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import run.ikaros.server.core.service.ScheduledTaskService;
import run.ikaros.server.entity.ScheduledTaskEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author li-guohao
 */
@Configuration
@EnableScheduling
public class TaskManagerConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskManagerConfig.class);

    private final ScheduledTaskService scheduledTaskService;

    public TaskManagerConfig(ScheduledTaskService scheduledTaskService) {
        this.scheduledTaskService = scheduledTaskService;
    }

    //    @Scheduled(cron = "0 */30 * * * ?")
    //    public void halfHourOnceTask() {
    //        taskService.pullAnimeSubscribeAndSaveMetadataAndDownloadTorrents();
    //        taskService.searchDownloadProcessAndCreateFileHardLinksAndRelateEpisode();
    //    }
    //
    //    @Scheduled(cron = "0 */5 * * * ?")
    //    public void fiveMinuteOnceTask() {
    //        qbittorrentClient.tryToResumeAllMissingFilesErroredTorrents();
    //    }


    @Scheduled(cron = "0 0 1 * * ?")
    public void scheduledTaskManage() {
        List<ScheduledTaskEntity> scheduledTaskEntities = scheduledTaskService.listAll();
        for (ScheduledTaskEntity taskEntity : scheduledTaskEntities) {
            if (LocalDateTime.now().isAfter(taskEntity.getDeadTime())) {
                scheduledTaskService.stop(taskEntity);
                scheduledTaskService.updateStatus(taskEntity.getId(), true);
                LOGGER.info("删除过期定时任务成功，ID:{}，名称：{}，Cron：{}，开始时间：{}，截止时间：{}",
                    taskEntity.getId(), taskEntity.getName(), taskEntity.getCron(),
                    taskEntity.getStartTime(), taskEntity.getDeadTime());
            } else {
                LOGGER.info("尝试启动尚未启动的定时任务，ID:{}，名称：{}，Cron：{}，开始时间：{}，截止时间：{}",
                    taskEntity.getId(), taskEntity.getName(), taskEntity.getCron(),
                    taskEntity.getStartTime(), taskEntity.getDeadTime());
                scheduledTaskService.start(taskEntity);
            }
        }
    }

}
