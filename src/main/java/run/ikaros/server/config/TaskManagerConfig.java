package run.ikaros.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.core.service.ScheduledTaskService;
import run.ikaros.server.core.service.TaskService;
import run.ikaros.server.entity.ScheduledTaskEntity;
import run.ikaros.server.enums.OptionApp;
import run.ikaros.server.enums.OptionCategory;
import run.ikaros.server.exceptions.RecordNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author li-guohao
 */
@Configuration
@EnableScheduling
public class TaskManagerConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskManagerConfig.class);

    private final OptionService optionService;
    private final TaskService taskService;


    public TaskManagerConfig(OptionService optionService,
                             TaskService taskService) {
        this.optionService = optionService;
        this.taskService = taskService;
    }

    private boolean appIsInit() {
        try {
            Boolean status = optionService.findOptionValueByCategoryAndKey(OptionCategory.APP,
                OptionApp.IS_INIT.name()).getStatus();
            LOGGER.debug("current app init status={}", status);
            return status;
        } catch (RecordNotFoundException recordNotFoundException) {
            LOGGER.debug("app not init, skip config cron task: updateAutoAnimeSubTaskStatus");
            return false;
        }
    }

    @Scheduled(cron = "0 */30 * * * ?")
    public void halfHourOnceTask() {
        LOGGER.debug("exec scheduled task: halfHourOnceTask");
        if (!appIsInit()) {
            LOGGER.debug("app not init, skip config cron task: halfHourOnceTask");
            return;
        }

        String value = optionService.findOptionValueByCategoryAndKey(OptionCategory.APP,
            OptionApp.ENABLE_AUTO_ANIME_SUB_TASK.name()).getValue();
        LOGGER.debug("current app ENABLE_AUTO_ANIME_SUB_TASK={}", value);
        if (Boolean.TRUE.toString().equalsIgnoreCase(value)) {
            LOGGER.debug("start exec task: "
                + "pull anime subscribe and save metadata and download torrents");
            taskService.pullAnimeSubscribeAndSaveMetadataAndDownloadTorrents();
            LOGGER.debug("end exec task: "
                + "pull anime subscribe and save metadata and download torrents");
        }

    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void fiveMinuteOnceTask() {
        LOGGER.debug("exec scheduled task: fiveMinuteOnceTask");
        if (!appIsInit()) {
            LOGGER.debug("app not init, skip config cron task: fiveMinuteOnceTask");
            return;
        }

        String value = optionService.findOptionValueByCategoryAndKey(OptionCategory.APP,
            OptionApp.ENABLE_AUTO_ANIME_SUB_TASK.name()).getValue();
        LOGGER.debug("current app ENABLE_AUTO_ANIME_SUB_TASK={}", value);
        if (Boolean.TRUE.toString().equalsIgnoreCase(value)) {
            LOGGER.debug("start exec task: "
                + "search download process and create file hard links and relate episode");
            taskService.searchDownloadProcessAndCreateFileHardLinksAndRelateEpisode();
            LOGGER.debug("end exec task: "
                + "search download process and create file hard links and relate episode");
        }
    }


    //    @Scheduled(cron = "0 0 1 * * ?")
    //    public void scheduledTaskManage() {
    //        List<ScheduledTaskEntity> scheduledTaskEntities = scheduledTaskService.listAll();
    //        for (ScheduledTaskEntity taskEntity : scheduledTaskEntities) {
    //            if (LocalDateTime.now().isAfter(taskEntity.getDeadTime())) {
    //                scheduledTaskService.stop(taskEntity);
    //                scheduledTaskService.updateStatus(taskEntity.getId(), true);
    //                LOGGER.info("删除过期定时任务成功，ID:{}，名称：{}，Cron：{}，开始时间：{}，截止时间：{}",
    //                    taskEntity.getId(), taskEntity.getName(), taskEntity.getCron(),
    //                    taskEntity.getStartTime(), taskEntity.getDeadTime());
    //            } else {
    //                LOGGER.info(
    //                    "尝试启动尚未启动的定时任务，ID:{}，名称：{}，Cron：{}，开始时间：{}，截止时间：{}",
    //                    taskEntity.getId(), taskEntity.getName(), taskEntity.getCron(),
    //                    taskEntity.getStartTime(), taskEntity.getDeadTime());
    //                scheduledTaskService.start(taskEntity);
    //            }
    //        }
    //    }


}
