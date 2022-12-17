package run.ikaros.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import run.ikaros.server.core.repository.ScheduledTaskRepository;
import run.ikaros.server.core.service.ScheduledTaskService;
import run.ikaros.server.entity.ScheduledTaskEntity;
import run.ikaros.server.utils.AssertUtils;

import jakarta.annotation.Nonnull;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Service
public class ScheduledTaskServiceImpl extends AbstractCrudService<ScheduledTaskEntity, Long>
    implements ScheduledTaskService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledTaskServiceImpl.class);

    private final ScheduledTaskRepository scheduledTaskRepository;
    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private Map<Long, ScheduledFuture<?>> scheduledFutureHashMap = new HashMap<>();

    public ScheduledTaskServiceImpl(ScheduledTaskRepository scheduledTaskRepository,
                                    ScheduledThreadPoolExecutor scheduledThreadPoolExecutor) {
        super(scheduledTaskRepository);
        this.scheduledTaskRepository = scheduledTaskRepository;
        this.scheduledThreadPoolExecutor = scheduledThreadPoolExecutor;
    }

    @Override
    public ScheduledTaskEntity start(@Nonnull ScheduledTaskEntity scheduledTaskEntity) {
        AssertUtils.notNull(scheduledTaskEntity, "scheduledTaskEntity");
        Long id = scheduledTaskEntity.getId();
        AssertUtils.notNull(id, "id");
        if (scheduledFutureHashMap.containsKey(id)) {
            LOGGER.info("跳过重复的定时任务：ID:{}，名称：{}，Cron：{}，开始时间：{}，截止时间：{}",
                scheduledTaskEntity.getId(), scheduledTaskEntity.getName(),
                scheduledTaskEntity.getCron(),
                scheduledTaskEntity.getStartTime(), scheduledTaskEntity.getDeadTime());
            return scheduledTaskEntity;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isEqual(scheduledTaskEntity.getStartTime())
            || now.isEqual(scheduledTaskEntity.getDeadTime())
            || (now.isAfter(scheduledTaskEntity.getStartTime())
            && now.isBefore(scheduledTaskEntity.getDeadTime()))) {

            String cron = scheduledTaskEntity.getCron();

            //            ScheduledFuture<?>
            //                future = scheduledThreadPoolExecutor
            //                .scheduleWithFixedDelay(new Runnable() {
            //                    @Override
            //                    public void run() {
            //                        LOGGER.info("exec scheduled task");
            //                    }
            //                }, new CronTrigger(cron));
            // scheduledFutureHashMap.put(id, future);

        }

        return null;
    }

    @Override
    public ScheduledTaskEntity stop(@Nonnull ScheduledTaskEntity scheduledTaskEntity) {
        AssertUtils.notNull(scheduledTaskEntity, "scheduledTaskEntity");

        return null;
    }

    @Override
    public ScheduledTaskEntity change(@Nonnull ScheduledTaskEntity scheduledTaskEntity) {
        AssertUtils.notNull(scheduledTaskEntity, "scheduledTaskEntity");

        return null;
    }

    @Override
    public ScheduledTaskEntity updateStatus(@Nonnull Long id, boolean status) {
        AssertUtils.notNull(id, "id");
        ScheduledTaskEntity scheduledTaskEntity = getById(id);
        if (scheduledTaskEntity == null) {
            return null;
        }
        scheduledTaskEntity.setStatus(status);
        return scheduledTaskRepository.saveAndFlush(scheduledTaskEntity);
    }
}
