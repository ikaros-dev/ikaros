package run.ikaros.operations.task;

import run.ikaros.common.PageResponse;
import run.ikaros.operations.api.BackgroundTask;
import run.ikaros.operations.api.BackgroundTaskService;
import run.ikaros.operations.api.TaskStatus;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface BackgroundTaskOperations extends BackgroundTaskService {
    Flux<BackgroundTask> list(TaskStatus status);
    Mono<PageResponse<BackgroundTask>> list(TaskStatus status, String taskType, int page, int size);
    Flux<BackgroundTaskAttemptEntity> attempts(UUID taskId);
}
