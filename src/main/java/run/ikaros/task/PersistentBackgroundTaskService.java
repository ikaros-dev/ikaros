package run.ikaros.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.PageResponse;
import run.ikaros.event.DurableEventService;

@Primary
@Service
public class PersistentBackgroundTaskService implements BackgroundTaskService {
    private static final int MAX_ATTEMPTS = 3;
    private final BackgroundTaskRepository tasks;
    private final BackgroundTaskAttemptRepository attempts;
    private final ObjectMapper mapper;
    private final DurableEventService events;
    private final DatabaseClient database;

    public PersistentBackgroundTaskService(BackgroundTaskRepository tasks, BackgroundTaskAttemptRepository attempts,
                                           ObjectMapper mapper, DurableEventService events, DatabaseClient database) {
        this.tasks = tasks; this.attempts = attempts; this.mapper = mapper; this.events = events; this.database = database;
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
        Mono<TaskSubmission> reused = existing.map(entity -> new TaskSubmission(entity, false));
        Mono<TaskSubmission> created = Mono.defer(() -> encode(payload).flatMap(json -> {
            Instant now = Instant.now();
            return tasks.save(new BackgroundTaskEntity(null, taskType, TaskStatus.PENDING.name(), json,
                idempotencyKey, now, timeoutAt(payload, now), null, null, null, 0, null, "{}", "{}", now, now, null))
                .onErrorResume(DuplicateKeyException.class, error -> tasks
                    .findByTaskTypeAndIdempotencyKey(taskType, idempotencyKey)
                    .switchIfEmpty(Mono.error(error)))
                .map(saved -> new TaskSubmission(saved, true));
        }));
        return reused.switchIfEmpty(created).flatMap(submission -> {
            BackgroundTaskEntity saved = submission.task();
            Mono<Void> createdEvent = submission.created() ? events.append("operations.background-task.created", 1,
                "background_task", saved.id(), "{\"task_id\":\"" + saved.id() + "\",\"task_type\":\""
                    + saved.taskType() + "\",\"status\":\"" + saved.status() + "\"}").then() : Mono.empty();
            return createdEvent.then(view(saved));
        });
    }

    @Override
    public Mono<BackgroundTask> claim(String runnerId, Duration leaseDuration) {
        if (runnerId == null || runnerId.isBlank() || leaseDuration == null || leaseDuration.isZero()
            || leaseDuration.isNegative()) return Mono.error(new IllegalArgumentException("Lease 参数不合法"));
        Instant observedAt = Instant.now();
        Instant leaseExpiresAt = observedAt.plus(leaseDuration);
        return expireDue(observedAt)
            .then(claimPending(runnerId, UUID.randomUUID(), observedAt, leaseExpiresAt))
            .switchIfEmpty(requeueExpired(observedAt)
                .then(claimPending(runnerId, UUID.randomUUID(), observedAt, leaseExpiresAt)))
            .switchIfEmpty(Mono.error(new NotFoundException("没有可执行的 Task")))
            .flatMap(claimed -> attempts.save(new BackgroundTaskAttemptEntity(null, claimed.id(), claimed.attempt(),
                TaskStatus.RUNNING.name(), runnerId, claimed.leaseExpiresAt(), observedAt, observedAt, null, null, observedAt))
                .then(events.append("operations.background-task.started", 1, "background_task", claimed.id(),
                    "{\"task_id\":\"" + claimed.id() + "\",\"attempt_no\":" + claimed.attempt() + "}"))
                .then(view(claimed)));
    }

    private Mono<Void> requeueExpired(Instant observedAt) {
        return tasks.findTop1ByStatusAndLeaseExpiresAtLessThanEqualOrderByLeaseExpiresAtAsc(TaskStatus.RUNNING.name(), observedAt)
            .flatMap(expired -> tasks.save(new BackgroundTaskEntity(expired.id(), expired.taskType(),
                expired.cancelRequestedAt() == null ? TaskStatus.PENDING.name() : TaskStatus.CANCELLED.name(),
                expired.payload(), expired.idempotencyKey(), observedAt, expired.timeoutAt(), null, null, null,
                expired.attempt(), expired.cancelRequestedAt(), expired.progress(), expired.result(), expired.createdAt(),
                Instant.now(), expired.parentTaskId()))
                .flatMap(requeued -> finishAttempt(expired,
                    expired.cancelRequestedAt() == null ? "LEASE_LOST" : TaskStatus.CANCELLED.name(), "Lease 已过期")
                    .then(expired.cancelRequestedAt() == null ? Mono.empty()
                        : events.append("operations.background-task.cancelled", 1, "background_task", requeued.id(),
                            "{\"task_id\":\"" + requeued.id() + "\",\"attempt_no\":" + requeued.attempt() + "}"))))
            .then();
    }

    private Mono<BackgroundTaskEntity> claimPending(String runnerId, UUID leaseToken, Instant now, Instant leaseExpiresAt) {
        return database.sql("""
            with candidate as (
                select id
                  from background_task
                 where status = 'PENDING' and cancel_requested_at is null and available_at <= :now
                 order by available_at asc, created_at asc, id asc
                 for update skip locked
                 limit 1
            )
            update background_task task
               set status = 'RUNNING', lease_owner = :runnerId, lease_token = :leaseToken,
                   lease_expires_at = :leaseExpiresAt, attempt = task.attempt + 1, updated_at = :now
              from candidate
             where task.id = candidate.id
         returning task.id, task.task_type, task.status, task.payload, task.idempotency_key,
                   task.available_at, task.timeout_at, task.lease_owner, task.lease_token,
                   task.lease_expires_at, task.attempt, task.cancel_requested_at, task.progress,
                   task.result_summary, task.created_at, task.updated_at, task.parent_task_id
            """)
            .bind("runnerId", runnerId)
            .bind("leaseToken", leaseToken)
            .bind("now", now)
            .bind("leaseExpiresAt", leaseExpiresAt)
            .map((row, metadata) -> new BackgroundTaskEntity(row.get("id", UUID.class), row.get("task_type", String.class),
                row.get("status", String.class), json(row.get("payload")), row.get("idempotency_key", String.class),
                row.get("available_at", Instant.class), row.get("timeout_at", Instant.class), row.get("lease_owner", String.class),
                row.get("lease_token", UUID.class), row.get("lease_expires_at", Instant.class), row.get("attempt", Integer.class),
                row.get("cancel_requested_at", Instant.class), json(row.get("progress")), json(row.get("result_summary")),
                row.get("created_at", Instant.class), row.get("updated_at", Instant.class), row.get("parent_task_id", UUID.class)))
            .one();
    }

    private String json(Object value) { return value == null ? "{}" : value.toString(); }

    private Mono<Void> expireDue(Instant now) {
        return reactor.core.publisher.Flux.concat(
            tasks.findAllByStatusAndTimeoutAtLessThanEqual(TaskStatus.PENDING.name(), now),
            tasks.findAllByStatusAndTimeoutAtLessThanEqual(TaskStatus.RUNNING.name(), now))
            .concatMap(task -> tasks.save(new BackgroundTaskEntity(task.id(), task.taskType(), TaskStatus.TIMED_OUT.name(),
                task.payload(), task.idempotencyKey(), task.availableAt(), task.timeoutAt(), null, null, null, task.attempt(),
                task.cancelRequestedAt(), task.progress(), "{\"code\":\"TASK_TIMEOUT\"}", task.createdAt(), Instant.now(), task.parentTaskId()))
                .flatMap(saved -> finishAttempt(task, TaskStatus.TIMED_OUT.name(), "TASK_TIMEOUT")
                    .then(events.append("operations.background-task.timed-out", 1, "background_task", saved.id(),
                        "{\"task_id\":\"" + saved.id() + "\",\"attempt_no\":" + saved.attempt() + "}"))))
            .then();
    }

    @Override
    public Mono<BackgroundTask> heartbeat(UUID taskId, UUID leaseToken, Duration leaseDuration) {
        return leased(taskId, leaseToken).flatMap(task -> tasks.save(copy(task, TaskStatus.RUNNING,
            task.leaseOwner(), task.leaseToken(), Instant.now().plus(leaseDuration), task.attempt(),
            task.cancelRequestedAt(), task.progress(), task.result()))).flatMap(this::view);
    }

    @Override
    public Mono<BackgroundTask> updateProgress(UUID taskId, UUID leaseToken, Map<String, Object> progress) {
        return leased(taskId, leaseToken)
            .flatMap(task -> encode(progress).flatMap(json -> tasks.save(copy(task, TaskStatus.valueOf(task.status()),
                task.leaseOwner(), task.leaseToken(), task.leaseExpiresAt(), task.attempt(),
                task.cancelRequestedAt(), json, task.result()))))
            .flatMap(this::view);
    }

    @Override
    public Mono<BackgroundTask> complete(UUID taskId, UUID leaseToken, Map<String, Object> result) {
        return leased(taskId, leaseToken).flatMap(task -> encode(result).flatMap(json -> tasks.save(copy(task,
            TaskStatus.SUCCEEDED, task.leaseOwner(), task.leaseToken(), task.leaseExpiresAt(), task.attempt(),
            task.cancelRequestedAt(), task.progress(), json))
            .flatMap(saved -> finishAttempt(task, TaskStatus.SUCCEEDED.name(), null)
                .then(events.append("operations.background-task.succeeded", 1, "background_task", saved.id(),
                    "{\"task_id\":\"" + saved.id() + "\",\"attempt_no\":" + saved.attempt() + "}"))
                .then(view(saved)))));
    }

    @Override
    public Mono<BackgroundTask> fail(UUID taskId, UUID leaseToken, Map<String, Object> error) {
        return leased(taskId, leaseToken)
            .flatMap(task -> encode(error).flatMap(json -> {
                boolean retryable = Boolean.TRUE.equals(error == null ? null : error.get("retryable")) && task.attempt() < MAX_ATTEMPTS;
                Instant availableAt = retryable ? Instant.now().plus(backoff(task.attempt())) : task.availableAt();
                BackgroundTaskEntity failed = new BackgroundTaskEntity(task.id(), task.taskType(),
                    retryable ? TaskStatus.PENDING.name() : TaskStatus.FAILED.name(), task.payload(), task.idempotencyKey(),
                    availableAt, task.timeoutAt(), retryable ? null : task.leaseOwner(), retryable ? null : task.leaseToken(),
                    retryable ? null : task.leaseExpiresAt(), task.attempt(), task.cancelRequestedAt(), task.progress(), json,
                    task.createdAt(), Instant.now(), task.parentTaskId());
                return tasks.save(failed).flatMap(saved -> finishAttempt(task, TaskStatus.FAILED.name(), message(error))
                    .then(events.append("operations.background-task.failed", 1, "background_task", saved.id(),
                        "{\"task_id\":\"" + saved.id() + "\",\"attempt_no\":" + saved.attempt()
                            + ",\"error_classification\":\"" + safe(error == null ? null : error.get("code"))
                            + "\",\"retryable\":" + retryable + "}"))
                    .then(view(saved)));
            }));
    }

    private Mono<Void> finishAttempt(BackgroundTaskEntity task, String status, String error) {
        return attempts.findByTaskIdAndAttemptNo(task.id(), task.attempt())
            .flatMap(old -> attempts.save(new BackgroundTaskAttemptEntity(old.id(), old.taskId(), old.attemptNo(), status,
                old.claimedBy(), old.leaseExpiresAt(), old.heartbeatAt(), old.startedAt(), Instant.now(), error, old.createdAt())))
            .then();
    }

    private String message(Map<String, Object> error) {
        Object value = error == null ? null : error.get("message");
        return value == null ? null : value.toString();
    }

    private String safe(Object value) {
        return value == null ? "unknown" : value.toString().replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Duration backoff(int attempt) { return Duration.ofSeconds(Math.min(3600L, 30L * (1L << Math.min(attempt, 7)))); }

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
                    now, timeoutAtJson(old.payload(), now), null, null, null, 0, null, "{}", "{}", now, now, old.id()));
            }).flatMap(saved -> events.append("operations.background-task.retry-requested", 1, "background_task", saved.parentTaskId(),
                "{\"task_id\":\"" + saved.parentTaskId() + "\",\"next_attempt_no\":" + (saved.attempt() + 1) + "}").then(view(saved)));
    }

    @Override
    public Mono<BackgroundTask> cancel(UUID taskId) {
        return tasks.findById(taskId).switchIfEmpty(Mono.error(new NotFoundException("Task 不存在")))
            .flatMap(task -> {
                TaskStatus status = TaskStatus.valueOf(task.status());
                if (status == TaskStatus.CANCELLED) return view(task);
                if (status == TaskStatus.SUCCEEDED || status == TaskStatus.FAILED || status == TaskStatus.TIMED_OUT) {
                    return Mono.error(new ConflictException("终态 Task 不能取消"));
                }
                TaskStatus target = status == TaskStatus.RUNNING ? TaskStatus.RUNNING : TaskStatus.CANCELLED;
                return tasks.save(copy(task, target, task.leaseOwner(), task.leaseToken(),
                    task.leaseExpiresAt(), task.attempt(), Instant.now(), task.progress(), task.result()))
                    .flatMap(saved -> events.append("operations.background-task.cancel-requested", 1, "background_task", saved.id(),
                        "{\"task_id\":\"" + saved.id() + "\"}").then(view(saved)));
            });
    }

    @Override
    public Mono<BackgroundTask> acknowledgeCancellation(UUID taskId, UUID leaseToken) {
        return leased(taskId, leaseToken).flatMap(task -> {
            if (task.cancelRequestedAt() == null) return Mono.error(new ConflictException("Task 尚未请求取消"));
            return tasks.save(copy(task, TaskStatus.CANCELLED, task.leaseOwner(), task.leaseToken(), task.leaseExpiresAt(),
                task.attempt(), task.cancelRequestedAt(), task.progress(), task.result()))
                .flatMap(saved -> finishAttempt(task, TaskStatus.CANCELLED.name(), null)
                    .then(events.append("operations.background-task.cancelled", 1, "background_task", saved.id(),
                        "{\"task_id\":\"" + saved.id() + "\",\"attempt_no\":" + saved.attempt() + "}"))
                    .then(view(saved)));
        });
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
                mapper.readValue(entity.payload(), new TypeReference<>() { }), entity.idempotencyKey(), entity.availableAt(), entity.timeoutAt(),
                entity.leaseOwner(), entity.leaseToken(), entity.leaseExpiresAt(), entity.attempt(),
                entity.cancelRequestedAt(), mapper.readValue(entity.progress(), new TypeReference<>() { }),
                mapper.readValue(entity.result(), new TypeReference<>() { }), entity.createdAt(), entity.updatedAt(), entity.parentTaskId()));
        } catch (JsonProcessingException | IllegalArgumentException error) {
            return Mono.error(new IllegalStateException("Task 数据损坏", error));
        }
    }

    private record TaskSubmission(BackgroundTaskEntity task, boolean created) {}

    private Mono<String> encode(Map<String, Object> value) {
        try { return Mono.just(mapper.writeValueAsString(value == null ? Map.of() : value)); }
        catch (JsonProcessingException error) { return Mono.error(new IllegalArgumentException("Task Payload 无法序列化", error)); }
    }

    private BackgroundTaskEntity copy(BackgroundTaskEntity old, TaskStatus status, String owner, UUID token,
                                      Instant expires, int attempt, Instant cancelled, String progress, String result) {
        return new BackgroundTaskEntity(old.id(), old.taskType(), status.name(), old.payload(), old.idempotencyKey(),
            old.availableAt(), old.timeoutAt(), owner, token, expires, attempt, cancelled, progress, result, old.createdAt(), Instant.now(), old.parentTaskId());
    }

    private Instant timeoutAt(Map<String, Object> payload, Instant createdAt) {
        Object value = payload == null ? null : payload.get("timeout_seconds");
        if (value == null) return null;
        try { long seconds = Long.parseLong(value.toString()); return seconds > 0 ? createdAt.plusSeconds(seconds) : null; }
        catch (NumberFormatException ignored) { return null; }
    }

    private Instant timeoutAtJson(String payload, Instant createdAt) {
        try { return timeoutAt(mapper.readValue(payload, new TypeReference<>() { }), createdAt); }
        catch (JsonProcessingException ignored) { return null; }
    }
}
