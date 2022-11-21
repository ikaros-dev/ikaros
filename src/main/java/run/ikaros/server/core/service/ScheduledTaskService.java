package run.ikaros.server.core.service;

import org.springframework.transaction.annotation.Transactional;
import run.ikaros.server.entity.ScheduledTaskEntity;

import javax.annotation.Nonnull;

public interface ScheduledTaskService extends CrudService<ScheduledTaskEntity, Long> {

    @Transactional
    ScheduledTaskEntity start(@Nonnull ScheduledTaskEntity scheduledTaskEntity);

    @Transactional
    ScheduledTaskEntity stop(@Nonnull ScheduledTaskEntity scheduledTaskEntity);

    @Transactional
    ScheduledTaskEntity change(@Nonnull ScheduledTaskEntity scheduledTaskEntity);

    @Transactional
    ScheduledTaskEntity updateStatus(@Nonnull Long id, boolean status);
}
