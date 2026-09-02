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

@RestController
@RequestMapping({"/api/background-tasks", "/api/v2/background-tasks"})
public class BackgroundTaskController {
    private final BackgroundTaskService service;

    public BackgroundTaskController(BackgroundTaskService service) {
        this.service = service;
    }

    @GetMapping
    public Flux<BackgroundTask> list(@RequestParam(required = false) TaskStatus status) {
        return service.list(status);
    }

    @GetMapping("/{taskId}")
    public Mono<BackgroundTask> get(@PathVariable UUID taskId) {
        return service.get(taskId);
    }

    @DeleteMapping("/{taskId}")
    public Mono<ResponseEntity<Void>> cancel(@PathVariable UUID taskId) {
        return service.cancel(taskId).thenReturn(ResponseEntity.noContent().build());
    }

    @GetMapping("/{taskId}/attempts")
    public Flux<BackgroundTaskAttemptEntity> attempts(@PathVariable UUID taskId) {
        return service.attempts(taskId);
    }
}
