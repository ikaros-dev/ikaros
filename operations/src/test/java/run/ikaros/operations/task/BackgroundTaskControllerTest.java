package run.ikaros.operations.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.operations.api.BackgroundTask;
import run.ikaros.operations.api.SubmitBackgroundTaskRequest;
import run.ikaros.operations.api.TaskStatus;

class BackgroundTaskControllerTest {
    @Test
    void submitsDurablyAndReturnsAcceptedLocation() {
        BackgroundTaskOperations service = mock(BackgroundTaskOperations.class);
        UUID id = UUID.randomUUID();
        BackgroundTask task = new BackgroundTask(id, "import", TaskStatus.PENDING, Map.of(), "request-1",
            Instant.now(), null, null, null, null, 0, null, Map.of(), Map.of(), Instant.now(), Instant.now(), null);
        when(service.submit(eq("import"), eq(Map.of("source", "drive")), eq("request-1")))
            .thenReturn(Mono.just(task));

        StepVerifier.create(new BackgroundTaskController(service).submit("request-1",
                new SubmitBackgroundTaskRequest("import", Map.of("source", "drive"))))
            .assertNext(response -> {
                org.junit.jupiter.api.Assertions.assertEquals(202, response.getStatusCode().value());
                org.junit.jupiter.api.Assertions.assertEquals("/api/background-tasks/" + id,
                    response.getHeaders().getLocation().toString());
                org.junit.jupiter.api.Assertions.assertEquals(id, response.getBody().id());
            })
            .verifyComplete();
        verify(service).submit("import", Map.of("source", "drive"), "request-1");
    }

    @Test
    void rejectsNullRequestBeforeCallingService() {
        BackgroundTaskOperations service = mock(BackgroundTaskOperations.class);
        StepVerifier.create(new BackgroundTaskController(service).submit(null, null))
            .expectError(IllegalArgumentException.class)
            .verify();
        verify(service, org.mockito.Mockito.never()).submit(any(), any(), any());
    }
}
