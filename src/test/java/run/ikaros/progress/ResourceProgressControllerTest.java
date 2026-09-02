package run.ikaros.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** 验证消费进度接口的服务调用。 */
class ResourceProgressControllerTest {
    private ResourceProgressService service;
    private ResourceProgressController controller;

    @BeforeEach
    void setUp() {
        service = mock(ResourceProgressService.class);
        controller = new ResourceProgressController(service);
    }

    @Test
    void setsProgress() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        SetProgressRequest request = new SetProgressRequest(ProgressType.AUDIO_SECONDS, 20L, 100L, false);
        ResourceProgressView view = new ResourceProgressView(UUID.randomUUID(), resourceId,
            ProgressType.AUDIO_SECONDS, 20L, 100L, false, null);
        when(service.set(ownerId, resourceId, request)).thenReturn(Mono.just(view));

        StepVerifier.create(controller.set(ownerId, resourceId, request))
            .assertNext(result -> assertThat(result).isEqualTo(view)).verifyComplete();
        verify(service).set(ownerId, resourceId, request);
    }

    @Test
    void getsProgress() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        when(service.get(ownerId, resourceId, ProgressType.READING_PAGES)).thenReturn(Mono.error(
            new RuntimeException("not found")));
        StepVerifier.create(controller.get(ownerId, resourceId, ProgressType.READING_PAGES))
            .expectErrorMessage("not found").verify();
    }
}
