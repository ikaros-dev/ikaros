package run.ikaros.server.core.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TaskInit {
    private final TaskService taskService;

    public TaskInit(TaskService taskService) {
        this.taskService = taskService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        taskService.updateAllRunningTaskStatusToCancel();
    }
}
