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
 * 验证 Resource 收藏接口的响应语义。
 */
class FavoriteControllerTest {
    private FavoriteService service;
    private FavoriteController controller;

    @BeforeEach
    void setUp() {
        service = mock(FavoriteService.class);
        controller = new FavoriteController(service);
    }

    @Test
    void addsFavorite() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        FavoriteView view = new FavoriteView(resourceId, true);
        when(service.add(ownerId, resourceId)).thenReturn(Mono.just(view));

        StepVerifier.create(controller.add(ownerId, resourceId))
            .assertNext(result -> assertThat(result).isEqualTo(view))
            .verifyComplete();
    }

    @Test
    void removesFavoriteWithNoContent() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        when(service.remove(ownerId, resourceId)).thenReturn(Mono.empty());

        StepVerifier.create(controller.remove(ownerId, resourceId))
            .assertNext(response -> assertThat(response).isEqualTo(ResponseEntity.noContent().build()))
            .verifyComplete();

        verify(service).remove(ownerId, resourceId);
    }
}
