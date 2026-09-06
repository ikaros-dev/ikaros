package run.ikaros.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** 验证 Activity 接口的响应语义。 */
class ResourceActivityControllerTest {
    private ResourceActivityService service;
    private ResourceActivityController controller;

    @BeforeEach
    void setUp() {
        service = mock(ResourceActivityService.class);
        controller = new ResourceActivityController(service);
    }

    @Test
    void returnsRecentActivities() {
        UUID ownerId = UUID.randomUUID();
        when(service.recent(ownerId, 5)).thenReturn(Mono.just(List.of()));
        StepVerifier.create(controller.recent(ownerId, 5))
            .assertNext(activities -> assertThat(activities).isEmpty()).verifyComplete();
        verify(service).recent(ownerId, 5);
    }

    @Test
    void deletesActivityWithNoContent() {
        UUID ownerId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        when(service.delete(ownerId, activityId)).thenReturn(Mono.empty());
        StepVerifier.create(controller.delete(ownerId, activityId))
            .assertNext(response -> assertThat(response).isEqualTo(ResponseEntity.noContent().build()))
            .verifyComplete();
    }
}
