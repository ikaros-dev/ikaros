package run.ikaros.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.PageResponse;

@Primary
@Service
public class PersistentBackgroundTaskService implements BackgroundTaskService {
    private final BackgroundTaskRepository tasks;
    private final BackgroundTaskAttemptRepository attempts;
    private final ObjectMapper mapper;

    public PersistentBackgroundTaskService(BackgroundTaskRepository tasks, BackgroundTaskAttemptRepository attempts,
                                           ObjectMapper mapper) {
        this.tasks = tasks; this.attempts = attempts; this.mapper = mapper;
    }

    @Override
    public Mono<BackgroundTask> get(UUID taskId) {
        return tasks.findById(taskId).switchIfEmpty(Mono.error(new NotFoundException("Task 不存在")))
            .flatMap(this::view);
    }

    @Override
    public Flux<BackgroundTask> list(TaskStatus status) {
        Flux<BackgroundTaskEntity> source = status == null ? tasks.findAll() :
            tasks.findAllByStatusOrderByCreatedAtDesc(status.name());
        return source.flatMap(this::view);
    }

    @Override
    public Mono<PageResponse<BackgroundTask>> list(TaskStatus status, String taskType, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            return Mono.error(new IllegalArgumentException("分页参数不合法"));
        }
        String type = taskType == null ? "" : taskType.trim();
        return list(status)
            .filter(task -> type.isEmpty() || task.taskType().equals(type))
            .collectList()
            .map(all -> new PageResponse<>(all.stream().skip((long) page * size).limit(size).toList(),
                all.size(), page, size));
    }

    @Override
    public Flux<BackgroundTaskAttemptEntity> attempts(UUID taskId) {
        return tasks.existsById(taskId).flatMapMany(exists -> exists
            ? attempts.findAllByTaskIdOrderByAttemptNoAsc(taskId)
            : Flux.error(new NotFoundException("Task 不存在")));
    }

    @Override
    public Mono<BackgroundTask> submit(String taskType, Map<String, Object> payload, String idempotencyKey) {
        if (taskType == null || taskType.isBlank()) return Mono.error(new IllegalArgumentException("Task 类型不能为空"));
        Mono<BackgroundTaskEntity> existing = idempotencyKey == null ? Mono.empty()
            : tasks.findByTaskTypeAndIdempotencyKey(taskType, idempotencyKey);
        return existing.switchIfEmpty(Mono.defer(() -> encode(payload).flatMap(json -> {
            Instant now = Instant.now();
            return tasks.save(new BackgroundTaskEntity(null, taskType, TaskStatus.PENDING.name(), json,
                idempotencyKey, now, null, null, null, 0, null, "{}", "{}", now, now, null));
        }))).flatMap(this::view);
    }

    @Override
    public Mono<BackgroundTask> claim(String runnerId, Duration leaseDuration) {
        if (runnerId == null || runnerId.isBlank() || leaseDuration == null || leaseDuration.isZero()
            || leaseDuration.isNegative()) return Mono.error(new IllegalArgumentException("Lease 参数不合法"));
        return tasks.findTop1ByStatusAndAvailableAtLessThanEqualOrderByAvailableAtAscCreatedAtAsc(
                TaskStatus.PENDING.name(), Instant.now())
            .switchIfEmpty(Mono.error(new NotFoundException("没有可执行的 Task")))
            .flatMap(task -> {
                Instant now = Instant.now(); UUID token = UUID.randomUUID();
                BackgroundTaskEntity claimed = copy(task, TaskStatus.RUNNING, runnerId, token,
                    now.plus(leaseDuration), task.attempt() + 1, task.cancelRequestedAt(), task.progress(), task.result());
                return tasks.save(claimed)
                    .then(attempts.save(new BackgroundTaskAttemptEntity(null, task.id(), task.attempt() + 1,
                        TaskStatus.RUNNING.name(), runnerId, claimed.leaseExpiresAt(), now, now, null, null, now)))
                    .then(view(claimed));
            });
    }

    @Override
    public Mono<BackgroundTask> heartbeat(UUID taskId, UUID leaseToken, Duration leaseDuration) {
        return leased(taskId, leaseToken).flatMap(task -> tasks.save(copy(task, TaskStatus.RUNNING,
            task.leaseOwner(), task.leaseToken(), Instant.now().plus(leaseDuration), task.attempt(),
            task.cancelRequestedAt(), task.progress(), task.result()))).flatMap(this::view);
    }

    @Override
    public Mono<BackgroundTask> complete(UUID taskId, UUID leaseToken, Map<String, Object> result) {
        return leased(taskId, leaseToken).flatMap(task -> encode(result).flatMap(json -> tasks.save(copy(task,
            TaskStatus.SUCCEEDED, task.leaseOwner(), task.leaseToken(), task.leaseExpiresAt(), task.attempt(),
            task.cancelRequestedAt(), task.progress(), json)))).flatMap(this::view);
    }

    @Override
    public Mono<BackgroundTask> fail(UUID taskId, UUID leaseToken, Map<String, Object> error) {
        return leased(taskId, leaseToken)
            .flatMap(task -> encode(error).flatMap(json -> tasks.save(copy(task, TaskStatus.FAILED,
                task.leaseOwner(), task.leaseToken(), task.leaseExpiresAt(), task.attempt(), task.cancelRequestedAt(),
                task.progress(), json))))
            .flatMap(this::view);
    }

    @Override
    public Mono<BackgroundTask> retry(UUID taskId) {
        return tasks.findById(taskId).switchIfEmpty(Mono.error(new NotFoundException("Task 不存在")))
            .flatMap(old -> {
                TaskStatus status = TaskStatus.valueOf(old.status());
                if (status != TaskStatus.FAILED && status != TaskStatus.TIMED_OUT) {
                    return Mono.error(new ConflictException("只有失败或超时 Task 可以人工重试"));
                }
                Instant now = Instant.now();
                return tasks.save(new BackgroundTaskEntity(null, old.taskType(), TaskStatus.PENDING.name(), old.payload(), null,
                    now, null, null, null, 0, null, "{}", "{}", now, now, old.id()));
            }).flatMap(this::view);
    }

    @Override
    public Mono<BackgroundTask> cancel(UUID taskId) {
        return tasks.findById(taskId).switchIfEmpty(Mono.error(new NotFoundException("Task 不存在")))
            .flatMap(task -> tasks.save(copy(task, TaskStatus.CANCELLED, task.leaseOwner(), task.leaseToken(),
                task.leaseExpiresAt(), task.attempt(), Instant.now(), task.progress(), task.result())))
            .flatMap(this::view);
    }

    private Mono<BackgroundTaskEntity> leased(UUID id, UUID token) {
        return tasks.findById(id).switchIfEmpty(Mono.error(new NotFoundException("Task 不存在"))).flatMap(task ->
            task.status().equals(TaskStatus.RUNNING.name()) && token != null && token.equals(task.leaseToken())
                && task.leaseExpiresAt() != null && task.leaseExpiresAt().isAfter(Instant.now())
                ? Mono.just(task) : Mono.error(new ConflictException("Task Lease 已失效")));
    }

    private Mono<BackgroundTask> view(BackgroundTaskEntity entity) {
        try {
            return Mono.just(new BackgroundTask(entity.id(), entity.taskType(), TaskStatus.valueOf(entity.status()),
                mapper.readValue(entity.payload(), new TypeReference<>() { }), entity.idempotencyKey(), entity.availableAt(),
                entity.leaseOwner(), entity.leaseToken(), entity.leaseExpiresAt(), entity.attempt(),
                entity.cancelRequestedAt(), mapper.readValue(entity.progress(), new TypeReference<>() { }),
                mapper.readValue(entity.result(), new TypeReference<>() { }), entity.createdAt(), entity.updatedAt(), entity.parentTaskId()));
        } catch (JsonProcessingException | IllegalArgumentException error) {
            return Mono.error(new IllegalStateException("Task 数据损坏", error));
        }
    }

    private Mono<String> encode(Map<String, Object> value) {
        try { return Mono.just(mapper.writeValueAsString(value == null ? Map.of() : value)); }
        catch (JsonProcessingException error) { return Mono.error(new IllegalArgumentException("Task Payload 无法序列化", error)); }
    }

    private BackgroundTaskEntity copy(BackgroundTaskEntity old, TaskStatus status, String owner, UUID token,
                                      Instant expires, int attempt, Instant cancelled, String progress, String result) {
        return new BackgroundTaskEntity(old.id(), old.taskType(), status.name(), old.payload(), old.idempotencyKey(),
            old.availableAt(), owner, token, expires, attempt, cancelled, progress, result, old.createdAt(), Instant.now(), old.parentTaskId());
    }
}
