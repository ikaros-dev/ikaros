package run.ikaros.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * 验证 Resource 标题接口的服务调用和响应状态。
 */
class ResourceTitleControllerTest {
    private ResourceTitleService service;
    private ResourceTitleController controller;

    @BeforeEach
    void setUp() {
        service = mock(ResourceTitleService.class);
        controller = new ResourceTitleController(service);
    }

    @Test
    void delegatesTitleUpdate() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        SetResourceTitleRequest request = new SetResourceTitleRequest("ja", "タイトル", false);
        ResourceTitleView view = new ResourceTitleView(UUID.randomUUID(), "ja", "タイトル", false);
        when(service.set(ownerId, resourceId, request)).thenReturn(Mono.just(view));

        StepVerifier.create(controller.set(ownerId, resourceId, request))
            .assertNext(result -> assertThat(result).isEqualTo(view))
            .verifyComplete();

        verify(service).set(ownerId, resourceId, request);
    }

    @Test
    void delegatesTitleDeletionAndReturnsNoContent() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID titleId = UUID.randomUUID();
        when(service.delete(ownerId, resourceId, titleId)).thenReturn(Mono.empty());

        StepVerifier.create(controller.delete(ownerId, resourceId, titleId))
            .assertNext(response -> assertThat(response).isEqualTo(ResponseEntity.noContent().build()))
            .verifyComplete();

        verify(service).delete(ownerId, resourceId, titleId);
    }
}
