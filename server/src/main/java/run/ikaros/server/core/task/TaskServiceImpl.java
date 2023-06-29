package run.ikaros.server.core.task;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.TaskEntity;
import run.ikaros.server.store.enums.TaskStatus;
import run.ikaros.server.store.repository.TaskRepository;

@Slf4j
@Service
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final ExecutorService executorService;
    private Map<String, Future<?>> futureMap = new HashMap<>();

    public TaskServiceImpl(TaskRepository taskRepository, ExecutorService executorService) {
        this.taskRepository = taskRepository;
        this.executorService = executorService;
    }

    /**
     * Update task status.
     */
    //@Async
    //@Scheduled(cron = "0/30 * *  * * ? ")
    public void updateTaskStatus() {
        // log.debug("exec updateTaskStatus");
        for (Map.Entry<String, Future<?>> entry : futureMap.entrySet()) {
            String name = entry.getKey();
            Future<?> future = entry.getValue();
            TaskStatus taskStatus;
            if (future.isDone()) {
                taskStatus = TaskStatus.FINISH;
            } else if (future.isCancelled()) {
                taskStatus = TaskStatus.CANCEL;
            } else {
                taskStatus = TaskStatus.RUNNING;
            }
            taskRepository.findByName(name)
                .flatMap(taskEntity -> {
                    TaskStatus status = taskEntity.getStatus();
                    if (!status.equals(taskStatus)) {
                        taskEntity.setStatus(taskStatus);
                        log.debug("update task[{}] status from [{}] to [{}].",
                            taskEntity.getId() + "-" + taskEntity.getName(),
                            status, taskStatus);
                    }
                    return taskRepository.save(taskEntity);
                })
                .subscribe();
        }
    }

    @Override
    public Mono<TaskEntity> findById(Long id) {
        Assert.isTrue(id > 0, "'id' must gt 0.");
        return taskRepository.findById(id);
    }

    @Override
    public Mono<TaskEntity> findByName(String name) {
        Assert.hasText(name, "'name' must has text.");
        return taskRepository.findByName(name);
    }

    @Override
    public Mono<Void> submit(Task task) {
        Assert.notNull(task, "'task' must not null.");
        TaskEntity entity = task.getEntity();
        Assert.notNull(entity, "'task entity' must not null.");
        setDefaultFieldValue(entity);
        Assert.hasText(entity.getName(), "'task entity name' must has text.");

        // 重复提交校验
        if (futureMap.containsKey(entity.getName()) && !futureMap.get(entity.getName()).isDone()) {
            throw new RuntimeException("Do not submit tasks twice for task: " + entity.getName());
        }

        return taskRepository.save(entity)
            // 目前直接后台执行，不进行延迟支持
            // .filter(te -> te.getCreateTime().equals(te.getStartTime()))
            .flatMap(taskEntity -> {
                futureMap.put(taskEntity.getName(), executorService.submit(task));
                return Mono.empty();
            });
    }

    private static void setDefaultFieldValue(TaskEntity entity) {
        if (entity.getStatus() == null) {
            entity.setStatus(TaskStatus.CREATE);
        }
        if (entity.getTotal() == null) {
            entity.setTotal(0L);
        }
        if (entity.getIndex() == null) {
            entity.setIndex(0L);
        }
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(LocalDateTime.now());
        }
    }

    @Override
    public Mono<Void> cancel(String name) {
        Assert.hasText(name, "'name' must has text.");
        Future<?> future = futureMap.get(name);
        future.cancel(true);
        return Mono.empty();
    }
}
