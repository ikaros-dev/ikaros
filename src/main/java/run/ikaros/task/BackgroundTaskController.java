package run.ikaros.task;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.PageResponse;

@RestController
@RequestMapping({"/api/background-tasks"})
public class BackgroundTaskController {
    private final BackgroundTaskService service;

    public BackgroundTaskController(BackgroundTaskService service) {
        this.service = service;
    }

    @GetMapping
    public Flux<BackgroundTask> list(@RequestParam(required = false) TaskStatus status) {
        return service.list(status);
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

    @GetMapping("/{taskId}/attempts")
    public Flux<BackgroundTaskAttemptEntity> attempts(@PathVariable UUID taskId) {
        return service.attempts(taskId);
    }
}
