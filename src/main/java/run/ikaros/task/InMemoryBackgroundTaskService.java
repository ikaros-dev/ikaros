package run.ikaros.task;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.PageResponse;

@Service
public class InMemoryBackgroundTaskService implements BackgroundTaskService {
    private static final int MAX_ATTEMPTS = 3;
    private final Map<UUID, BackgroundTask> tasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ConcurrentMap<Integer, BackgroundTaskAttemptEntity>> attemptHistory = new ConcurrentHashMap<>();

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
        return get(taskId).thenMany(Flux.defer(() -> Flux.fromIterable(attemptHistory
            .getOrDefault(taskId, new ConcurrentHashMap<>()).values()).sort(Comparator.comparingInt(BackgroundTaskAttemptEntity::attemptNo))));
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
                idempotencyKey, now, null, null, null, 0, null, Map.of(), Map.of(), now, now, null);
            tasks.put(task.id(), task);
            return task;
        });
    }

    @Override
    public Mono<BackgroundTask> claim(String runnerId, Duration leaseDuration) {
        if (runnerId == null || runnerId.isBlank() || leaseDuration == null || leaseDuration.isNegative()
            || leaseDuration.isZero()) return Mono.error(new IllegalArgumentException("Lease 参数不合法"));
        return Mono.defer(() -> {
            Instant observedAt = Instant.now();
            tasks.values().stream()
                .filter(task -> task.status() == TaskStatus.RUNNING && task.leaseExpiresAt() != null
                    && !task.leaseExpiresAt().isAfter(observedAt))
                .findFirst()
                .ifPresent(task -> {
                    finishAttempt(task, "LEASE_LOST", "Lease 已过期");
                    tasks.replace(task.id(), task, new BackgroundTask(task.id(), task.taskType(), TaskStatus.PENDING,
                        task.payload(), task.idempotencyKey(), observedAt, null, null, null, task.attempt(), task.cancelRequestedAt(),
                        task.progress(), task.result(), task.createdAt(), observedAt, task.parentTaskId()));
                });
            return tasks.values().stream()
            .filter(task -> task.status() == TaskStatus.PENDING && !task.availableAt().isAfter(observedAt))
            .sorted(Comparator.comparing(BackgroundTask::availableAt).thenComparing(BackgroundTask::createdAt))
            .findFirst()
            .map(task -> {
                Instant now = Instant.now();
                BackgroundTask claimed = copy(task, TaskStatus.RUNNING, runnerId, UUID.randomUUID(),
                    now.plus(leaseDuration), task.attempt() + 1, task.cancelRequestedAt(), task.progress(), task.result());
                tasks.replace(task.id(), task, claimed);
                attemptHistory.computeIfAbsent(task.id(), ignored -> new ConcurrentHashMap<>()).put(claimed.attempt(),
                    new BackgroundTaskAttemptEntity(UUID.randomUUID(), task.id(), claimed.attempt(), TaskStatus.RUNNING.name(),
                        runnerId, null, claimed.leaseExpiresAt(), now, now, null, now));
                return Mono.just(claimed);
            }).orElseGet(Mono::empty);
        });
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
            tasks.replace(task.id(), task, updated); finishAttempt(task, TaskStatus.SUCCEEDED.name(), null); return updated;
        });
    }

    @Override
    public Mono<BackgroundTask> fail(UUID taskId, UUID leaseToken, Map<String, Object> error) {
        return leased(taskId, leaseToken).map(task -> {
            boolean retryable = Boolean.TRUE.equals(error == null ? null : error.get("retryable")) && task.attempt() < MAX_ATTEMPTS;
            Instant availableAt = retryable ? Instant.now().plus(backoff(task.attempt())) : task.availableAt();
            BackgroundTask updated = new BackgroundTask(task.id(), task.taskType(), retryable ? TaskStatus.PENDING : TaskStatus.FAILED,
                task.payload(), task.idempotencyKey(), availableAt, retryable ? null : task.leaseOwner(),
                retryable ? null : task.leaseToken(), retryable ? null : task.leaseExpiresAt(), task.attempt(),
                task.cancelRequestedAt(), task.progress(), error, task.createdAt(), Instant.now(), task.parentTaskId());
            tasks.replace(task.id(), task, updated); finishAttempt(task, TaskStatus.FAILED.name(),
                error == null ? null : String.valueOf(error.get("message"))); return updated;
        });
    }

    private Duration backoff(int attempt) { return Duration.ofSeconds(Math.min(3600L, 30L * (1L << Math.min(attempt, 7)))); }

    private void finishAttempt(BackgroundTask task, String status, String error) {
        ConcurrentMap<Integer, BackgroundTaskAttemptEntity> history = attemptHistory.get(task.id());
        if (history == null) return;
        history.computeIfPresent(task.attempt(), (number, old) -> new BackgroundTaskAttemptEntity(old.id(), old.taskId(), old.attemptNo(),
            status, old.claimedBy(), old.leaseExpiresAt(), old.heartbeatAt(), old.startedAt(), Instant.now(), error, old.createdAt()));
    }

    @Override
    public Mono<BackgroundTask> retry(UUID taskId) {
        return get(taskId).flatMap(old -> {
            if (old.status() != TaskStatus.FAILED && old.status() != TaskStatus.TIMED_OUT) {
                return Mono.error(new ConflictException("只有失败或超时 Task 可以人工重试"));
            }
            Instant now = Instant.now();
            BackgroundTask retry = new BackgroundTask(UUID.randomUUID(), old.taskType(), TaskStatus.PENDING, old.payload(),
                null, now, null, null, null, 0, null, Map.of(), Map.of(), now, now, old.id());
            tasks.put(retry.id(), retry);
            return Mono.just(retry);
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
            owner, token, expires, attempt, cancelled, progress, result, old.createdAt(), Instant.now(), old.parentTaskId());
    }
}
