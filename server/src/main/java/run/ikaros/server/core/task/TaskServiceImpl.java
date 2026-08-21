package run.ikaros.server.core.task;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.ikaros.api.store.enums.TaskStatus;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.store.entity.TaskEntity;
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
    @Async
    @Scheduled(cron = "0/5 * *  * * ? ")
    public void updateTaskStatus() {
        // log.debug("exec updateTaskStatus");
        HashSet<Map.Entry<String, Future<?>>> entries = new HashSet<>(futureMap.entrySet());
        for (Map.Entry<String, Future<?>> entry : entries) {
            String name = entry.getKey();
            Future<?> future = entry.getValue();

            TaskStatus taskStatus;
            if (future.isDone()) {
                taskStatus = TaskStatus.FINISH;
                futureMap.remove(name);
            } else if (future.isCancelled()) {
                taskStatus = TaskStatus.CANCEL;
                futureMap.remove(name);
            } else {
                taskStatus = TaskStatus.RUNNING;
            }
            taskRepository.findAllByName(name)
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
    public void updateAllRunningAndCreatedTaskStatusToCancel() {
        taskRepository.findAllByStatus(TaskStatus.RUNNING)
            .flatMap(taskEntity -> taskRepository.save(taskEntity.setStatus(TaskStatus.CANCEL)
                .setFailMessage("Application stop.")))
            .thenMany(taskRepository.findAllByStatus(TaskStatus.CREATE))
            .flatMap(taskEntity -> taskRepository.save(taskEntity.setStatus(TaskStatus.CANCEL)
                .setFailMessage("Application stop.")))
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe();
    }

    @Override
    public Mono<TaskEntity> findById(UUID id) {
        Assert.notNull(id, "'id' must not null.");
        return taskRepository.findById(id);
    }


    @Override
    public Mono<Void> submit(Task task) {
        Assert.notNull(task, "'task' must not null.");
        TaskEntity entity = task.getEntity();
        Assert.notNull(entity, "'task entity' must not null.");
        entity.setName(task.getTaskEntityName());
        setDefaultFieldValue(entity);
        Assert.hasText(entity.getName(), "'task entity name' must has text.");

        // 重复提交校验
        if (futureMap.containsKey(entity.getName()) && !futureMap.get(entity.getName()).isDone()) {
            throw new RuntimeException("Do not submit tasks twice for task: " + entity.getName());
        }

        // 是否已经存在未运行完成的拉取任务
        return taskRepository.findAllByName(task.getTaskEntityName())
            .filter(taskEntity -> TaskStatus.RUNNING.equals(taskEntity.getStatus())
                || TaskStatus.CREATE.equals(taskEntity.getStatus()))
            .collectList()
            .filter(taskEntities -> taskEntities != null && !taskEntities.isEmpty())
            .flatMap(
                taskEntities -> Mono.error(new RuntimeException("Submission failed, task exists.")))
            .switchIfEmpty(taskRepository.save(entity)
                .flatMap(taskEntity -> {
                    Future<?> future = executorService.submit(task);
                    futureMap.put(taskEntity.getName(), future);
                    return Mono.empty();
                })
            ).then();
    }

    /**
     * Set Default filed for task entity.
     */
    public void setDefaultFieldValue(TaskEntity entity) {
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

    @Override
    public Mono<PagingWrap<TaskEntity>> listEntitiesByCondition(
        FindTaskCondition condition) {
        Assert.notNull(condition, "'condition' must no null.");

        final Integer page = condition.getPage();
        Assert.isTrue(page > 0, "'page' must gt 0.");

        final Integer size = condition.getSize();
        Assert.isTrue(size > 0, "'size' must gt 0.");

        final String name = StringUtils.hasText(condition.getName())
            ? condition.getName() : "";
        final String nameLike = "%" + name + "%";
        final TaskStatus taskStatus = condition.getStatus();

        PageRequest pageRequest = PageRequest.of(page - 1, size);

        Flux<TaskEntity> taskEntityFlux;
        Mono<Long> countMono;

        if (taskStatus == null) {
            taskEntityFlux = taskRepository.findAllByNameLike(nameLike, pageRequest);
            countMono = taskRepository.countAllByNameLike(nameLike);
        } else {
            taskEntityFlux =
                taskRepository.findAllByNameLikeAndStatus(nameLike, taskStatus, pageRequest);
            countMono = taskRepository.countAllByNameLikeAndStatus(nameLike, taskStatus);
        }
        Mono<Long> finalCountMono = countMono;
        return taskEntityFlux
            // 时间从近到远排序
            .sort((o1, o2) -> o2.getCreateTime().compareTo(o1.getCreateTime()))
            .collectList()
            .flatMap(taskEntities -> finalCountMono
                .map(count -> new PagingWrap<>(page,
                    size, count, taskEntities)));
    }

    @Override
    public Mono<Long> getProcess(UUID id) {
        Assert.notNull(id, "'id' must not null.");
        return findById(id)
            .filter(taskEntity -> taskEntity.getTotal() != 0)
            .map(taskEntity -> 100 * taskEntity.getIndex() / taskEntity.getTotal())
            .switchIfEmpty(Mono.just(0L));
    }
}
