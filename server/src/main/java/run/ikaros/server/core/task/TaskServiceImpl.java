package run.ikaros.server.core.task;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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
    public void updateAllRunningTaskStatusToCancel() {
        taskRepository.findAllByStatus(TaskStatus.RUNNING)
            .flatMap(taskEntity -> taskRepository.save(taskEntity.setStatus(TaskStatus.CANCEL)))
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe();
    }

    @Override
    public Mono<TaskEntity> findById(Long id) {
        Assert.isTrue(id > 0, "'id' must gt 0.");
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
            .filter(taskEntities -> taskEntities != null && taskEntities.size() > 0)
            .flatMap(
                taskEntities -> Mono.error(new RuntimeException("Submission failed, task exists.")))
            .switchIfEmpty(taskRepository.save(entity)
                .flatMap(taskEntity -> {
                    futureMap.put(taskEntity.getName(), executorService.submit(task));
                    return Mono.empty();
                })
            ).then();
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
    public Mono<Long> getProcess(Long id) {
        return findById(id)
            .filter(taskEntity -> taskEntity.getTotal() != 0)
            .map(taskEntity -> 100 * taskEntity.getIndex() / taskEntity.getTotal())
            .switchIfEmpty(Mono.just(0L));
    }
}
