package run.ikaros.operations.task;

import java.util.UUID;
import java.util.Map;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.PageResponse;
import run.ikaros.operations.api.BackgroundTask;
import run.ikaros.operations.api.TaskStatus;
import run.ikaros.operations.api.SubmitBackgroundTaskRequest;

@RestController
@RequestMapping({"/api/background-tasks"})
public class BackgroundTaskController {
    private final BackgroundTaskOperations service;

    public BackgroundTaskController(BackgroundTaskOperations service) {
        this.service = service;
    }

    @GetMapping
    public Flux<BackgroundTask> list(@RequestParam(required = false) TaskStatus status) {
        return service.list(status);
    }

    @PostMapping
    public Mono<ResponseEntity<BackgroundTask>> submit(
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @RequestBody SubmitBackgroundTaskRequest request
    ) {
        if (request == null) {
            return Mono.error(new IllegalArgumentException("Task 请求不能为空"));
        }
        return service.submit(request.type(), request.payload(), idempotencyKey)
            .map(task -> ResponseEntity.accepted()
                .location(URI.create("/api/background-tasks/" + task.id()))
                .body(task));
    }

    @GetMapping(params = "page")
    public Mono<PageResponse<BackgroundTask>> listPage(
        @RequestParam(required = false) TaskStatus status,
        @RequestParam(required = false, name = "task_type") String taskType,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return service.list(status, taskType, page, size);
    }

    @GetMapping("/{taskId}")
    public Mono<BackgroundTask> get(@PathVariable UUID taskId) {
        return service.get(taskId);
    }

    @DeleteMapping("/{taskId}")
    public Mono<ResponseEntity<Void>> cancel(@PathVariable UUID taskId) {
        return service.cancel(taskId).thenReturn(ResponseEntity.noContent().build());
    }

    @org.springframework.web.bind.annotation.PostMapping("/{taskId}/actions/cancel")
    public Mono<BackgroundTask> cancelAction(@PathVariable UUID taskId) {
        return service.cancel(taskId).then(service.get(taskId));
    }

    @org.springframework.web.bind.annotation.PostMapping("/{taskId}/actions/retry")
    public Mono<BackgroundTask> retry(@PathVariable UUID taskId) {
        return service.retry(taskId);
    }

    @PostMapping("/{taskId}/actions/progress")
    public Mono<BackgroundTask> progress(@PathVariable UUID taskId,
                                         @RequestHeader("X-Task-Lease-Token") UUID leaseToken,
                                         @RequestBody Map<String, Object> progress) {
        return service.updateProgress(taskId, leaseToken, progress);
    }

    @GetMapping("/{taskId}/attempts")
    public Flux<BackgroundTaskAttemptEntity> attempts(@PathVariable UUID taskId) {
        return service.attempts(taskId);
    }
}
