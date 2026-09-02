package run.ikaros.task;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;

/** 统一的 Task claim/handler/complete 运行链，业务 Handler 不直接操作 Task persistence。 */
@Service
public class BackgroundTaskDispatcher {
    private final BackgroundTaskService tasks;
    private final Map<String, BackgroundTaskHandler> handlers = new ConcurrentHashMap<>();

    public BackgroundTaskDispatcher(BackgroundTaskService tasks) { this.tasks = tasks; }

    public void register(String taskType, BackgroundTaskHandler handler) {
        if (taskType == null || taskType.isBlank() || handler == null) {
            throw new IllegalArgumentException("Task Handler 注册参数不合法");
        }
        if (handlers.putIfAbsent(taskType, handler) != null) {
            throw new ConflictException("Task Handler 已注册: " + taskType);
        }
    }

    public Mono<BackgroundTask> dispatchOnce(String runnerId, Duration leaseDuration) {
        return tasks.claim(runnerId, leaseDuration).flatMap(task -> {
            BackgroundTaskHandler handler = handlers.get(task.taskType());
            if (handler == null) return Mono.error(new ConflictException("未注册 Task Handler: " + task.taskType()));
            return handler.handle(task).defaultIfEmpty(Map.of())
                .flatMap(result -> tasks.complete(task.id(), task.leaseToken(), result));
        });
    }
}
