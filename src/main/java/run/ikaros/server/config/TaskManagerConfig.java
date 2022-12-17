package run.ikaros.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import run.ikaros.server.core.service.MediaService;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.core.service.TaskService;
import run.ikaros.server.entity.OptionEntity;
import run.ikaros.server.enums.OptionApp;
import run.ikaros.server.enums.OptionCategory;
import run.ikaros.server.model.dto.OptionQbittorrentDTO;
import run.ikaros.server.utils.StringUtils;

/**
 * @author li-guohao
 */
@Configuration
@EnableScheduling
public class TaskManagerConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskManagerConfig.class);

    private final OptionService optionService;
    private final TaskService taskService;
    private final MediaService mediaService;


    public TaskManagerConfig(OptionService optionService,
                             TaskService taskService, MediaService mediaService) {
        this.optionService = optionService;
        this.taskService = taskService;
        this.mediaService = mediaService;
    }

    private boolean appIsInit() {
        OptionEntity optionEntity =
            optionService.findOptionValueByCategoryAndKey(OptionCategory.APP,
                OptionApp.IS_INIT.name());

        boolean isInit = (optionEntity != null
            && Boolean.TRUE.toString().equalsIgnoreCase(optionEntity.getValue()));

        LOGGER.debug("current app init status={}", isInit);
        return isInit;
    }

    @Scheduled(cron = "0 */30 * * * ?")
    public void halfHourOnceTask() {
        LOGGER.debug("exec scheduled task: halfHourOnceTask");
        if (!appIsInit()) {
            LOGGER.debug("app not init, skip config cron task: halfHourOnceTask");
            return;
        }

        OptionEntity optionEntity =
            optionService.findOptionValueByCategoryAndKey(OptionCategory.APP,
                OptionApp.ENABLE_AUTO_ANIME_SUB_TASK.name());
        if (optionEntity != null) {
            String value = optionEntity.getValue();
            LOGGER.debug("current app ENABLE_AUTO_ANIME_SUB_TASK={}", value);
            if (Boolean.TRUE.toString().equalsIgnoreCase(value)) {
                LOGGER.debug("start exec task: "
                    + "pull anime subscribe and save metadata and download torrents");
                taskService.pullMikanRssAnimeSubscribeAndSaveMetadataAndDownloadTorrents();
                LOGGER.debug("end exec task: "
                    + "pull anime subscribe and save metadata and download torrents");
            }
        }


    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void fiveMinuteOnceTask() {
        LOGGER.debug("exec scheduled task: fiveMinuteOnceTask");
        if (!appIsInit()) {
            LOGGER.debug("app not init, skip config cron task: fiveMinuteOnceTask");
            return;
        }

        OptionEntity enableAutoAnimeSubTaskOptionEntity =
            optionService.findOptionValueByCategoryAndKey(OptionCategory.APP,
                OptionApp.ENABLE_AUTO_ANIME_SUB_TASK.name());

        if (enableAutoAnimeSubTaskOptionEntity != null) {
            String value = enableAutoAnimeSubTaskOptionEntity.getValue();
            LOGGER.debug("current app ENABLE_AUTO_ANIME_SUB_TASK={}", value);
            OptionQbittorrentDTO optionQbittorrentDTO = optionService.getOptionQbittorrentDTO();

            // 需要配置好了QB
            if (Boolean.TRUE.toString().equalsIgnoreCase(value)
                && StringUtils.isNotBlank(optionQbittorrentDTO.getUrlPrefix())
                && optionQbittorrentDTO.getUrlPrefix().startsWith("http")) {
                // 查询下载进度并创建文件硬链接任务
                taskService.searchDownloadProcessAndCreateFileHardLinksAndRelateEpisode();

                // 新番特征资源匹配任务
                // taskService.downloadSubscribeAnimeResource(null);
            }
        }

        OptionEntity enableGenerateMediaDirOptionEntity =
            optionService.findOptionValueByCategoryAndKey(OptionCategory.APP,
                OptionApp.ENABLE_GENERATE_MEDIA_DIR_TASK.name());
        if (enableGenerateMediaDirOptionEntity != null) {
            String enableGenerateMediaDir = enableGenerateMediaDirOptionEntity.getValue();
            LOGGER.debug("current app ENABLE_GENERATE_MEDIA_DIR_TASK={}", enableGenerateMediaDir);
            if (Boolean.TRUE.toString().equalsIgnoreCase(enableGenerateMediaDir)) {
                mediaService.generateMediaDir();
            }
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
