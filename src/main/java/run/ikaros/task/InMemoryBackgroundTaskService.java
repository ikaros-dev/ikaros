package run.ikaros.task;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.PageResponse;

@Service
public class InMemoryBackgroundTaskService implements BackgroundTaskService {
    private final Map<UUID, BackgroundTask> tasks = new ConcurrentHashMap<>();

    @Override
    public Mono<BackgroundTask> get(UUID taskId) {
        return Mono.justOrEmpty(tasks.get(taskId)).switchIfEmpty(Mono.error(new NotFoundException("Task 不存在")));
    }

    @Override
    public Flux<BackgroundTask> list(TaskStatus status) {
        return Flux.fromIterable(tasks.values()).filter(task -> status == null || task.status() == status);
    }

    @Override
    public Mono<PageResponse<BackgroundTask>> list(TaskStatus status, String taskType, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            return Mono.error(new IllegalArgumentException("分页参数不合法"));
        }
        String type = taskType == null ? "" : taskType.trim();
        return list(status)
            .filter(task -> type.isEmpty() || task.taskType().equals(type))
            .sort(Comparator.comparing(BackgroundTask::createdAt).reversed())
            .collectList()
            .map(all -> new PageResponse<>(all.stream().skip((long) page * size).limit(size).toList(),
                all.size(), page, size));
    }

    @Override
    public Flux<BackgroundTaskAttemptEntity> attempts(UUID taskId) {
        return get(taskId).thenMany(Flux.empty());
    }

    @Override
    public Mono<BackgroundTask> submit(String taskType, Map<String, Object> payload, String idempotencyKey) {
        if (taskType == null || taskType.isBlank()) return Mono.error(new IllegalArgumentException("Task 类型不能为空"));
        return Mono.fromSupplier(() -> {
            if (idempotencyKey != null) {
                BackgroundTask existing = tasks.values().stream()
                    .filter(task -> task.taskType().equals(taskType) && idempotencyKey.equals(task.idempotencyKey()))
                    .findFirst().orElse(null);
                if (existing != null) return existing;
            }
            Instant now = Instant.now();
            BackgroundTask task = new BackgroundTask(UUID.randomUUID(), taskType, TaskStatus.PENDING, payload,
                idempotencyKey, now, null, null, null, 0, null, Map.of(), Map.of(), now, now);
            tasks.put(task.id(), task);
            return task;
        });
    }

    @Override
    public Mono<BackgroundTask> claim(String runnerId, Duration leaseDuration) {
        if (runnerId == null || runnerId.isBlank() || leaseDuration == null || leaseDuration.isNegative()
            || leaseDuration.isZero()) return Mono.error(new IllegalArgumentException("Lease 参数不合法"));
        return Mono.defer(() -> tasks.values().stream()
            .filter(task -> task.status() == TaskStatus.PENDING && !task.availableAt().isAfter(Instant.now()))
            .sorted(Comparator.comparing(BackgroundTask::availableAt).thenComparing(BackgroundTask::createdAt))
            .findFirst()
            .map(task -> {
                Instant now = Instant.now();
                BackgroundTask claimed = copy(task, TaskStatus.RUNNING, runnerId, UUID.randomUUID(),
                    now.plus(leaseDuration), task.attempt() + 1, task.cancelRequestedAt(), task.progress(), task.result());
                tasks.replace(task.id(), task, claimed);
                return Mono.just(claimed);
            }).orElseGet(Mono::empty));
    }

    @Override
    public Mono<BackgroundTask> heartbeat(UUID taskId, UUID leaseToken, Duration leaseDuration) {
        return leased(taskId, leaseToken).map(task -> {
            BackgroundTask updated = copy(task, task.status(), task.leaseOwner(), task.leaseToken(),
                Instant.now().plus(leaseDuration), task.attempt(), task.cancelRequestedAt(), task.progress(), task.result());
            tasks.replace(task.id(), task, updated); return updated;
        });
    }

    @Override
    public Mono<BackgroundTask> complete(UUID taskId, UUID leaseToken, Map<String, Object> result) {
        return leased(taskId, leaseToken).map(task -> {
            BackgroundTask updated = copy(task, TaskStatus.SUCCEEDED, task.leaseOwner(), task.leaseToken(),
                task.leaseExpiresAt(), task.attempt(), task.cancelRequestedAt(), task.progress(), result);
            tasks.replace(task.id(), task, updated); return updated;
        });
    }

    @Override
    public Mono<BackgroundTask> fail(UUID taskId, UUID leaseToken, Map<String, Object> error) {
        return leased(taskId, leaseToken).map(task -> {
            BackgroundTask updated = copy(task, TaskStatus.FAILED, task.leaseOwner(), task.leaseToken(),
                task.leaseExpiresAt(), task.attempt(), task.cancelRequestedAt(), task.progress(), error);
            tasks.replace(task.id(), task, updated); return updated;
        });
    }

    @Override
    public Mono<BackgroundTask> cancel(UUID taskId) {
        return get(taskId).map(task -> {
            if (task.status() == TaskStatus.SUCCEEDED || task.status() == TaskStatus.FAILED) {
                throw new ConflictException("终态 Task 不能取消");
            }
            BackgroundTask updated = copy(task, TaskStatus.CANCELLED, task.leaseOwner(), task.leaseToken(),
                task.leaseExpiresAt(), task.attempt(), Instant.now(), task.progress(), task.result());
            tasks.replace(task.id(), task, updated); return updated;
        });
    }

    private Mono<BackgroundTask> leased(UUID id, UUID token) {
        return get(id).flatMap(task -> task.status() == TaskStatus.RUNNING && token != null
            && token.equals(task.leaseToken()) && task.leaseExpiresAt().isAfter(Instant.now())
            ? Mono.just(task) : Mono.error(new ConflictException("Task Lease 已失效")));
    }

    private BackgroundTask copy(BackgroundTask old, TaskStatus status, String owner, UUID token, Instant expires,
                                int attempt, Instant cancelled, Map<String, Object> progress, Map<String, Object> result) {
        return new BackgroundTask(old.id(), old.taskType(), status, old.payload(), old.idempotencyKey(), old.availableAt(),
            owner, token, expires, attempt, cancelled, progress, result, old.createdAt(), Instant.now());
    }
}
