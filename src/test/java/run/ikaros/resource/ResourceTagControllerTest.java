package run.ikaros.resource;

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

/** 验证 Resource 标签接口的服务调用与响应。 */
class ResourceTagControllerTest {
    private ResourceTagService service;
    private ResourceTagController controller;

    @BeforeEach
    void setUp() {
        service = mock(ResourceTagService.class);
        controller = new ResourceTagController(service);
    }

    @Test
    void listsTags() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        when(service.list(ownerId, resourceId)).thenReturn(Mono.just(List.of()));
        StepVerifier.create(controller.list(ownerId, resourceId))
            .assertNext(tags -> assertThat(tags).isEmpty()).verifyComplete();
        verify(service).list(ownerId, resourceId);
    }

    @Test
    void removesTagWithNoContent() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();
        when(service.remove(ownerId, resourceId, tagId)).thenReturn(Mono.empty());
        StepVerifier.create(controller.remove(ownerId, resourceId, tagId))
            .assertNext(response -> assertThat(response).isEqualTo(ResponseEntity.noContent().build()))
            .verifyComplete();
    }
}
