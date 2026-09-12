package run.ikaros.resource;

import run.ikaros.resource.api.ResourceLifecycle;
import run.ikaros.resource.api.ResourceService;
import run.ikaros.resource.api.ResourceType;
import run.ikaros.resource.api.ResourceView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.common.PageResponse;

/** 验证移动端资源浏览复用的 HTTP 查询契约。 */
class ResourceControllerTest {
    private ResourceService service;
    private ResourceController controller;

    @BeforeEach
    void setUp() {
        service = mock(ResourceService.class);
        controller = new ResourceController(service);
    }

    @Test
    void listsOwnerResourcesWithFiltersAndPaging() {
        UUID ownerId = UUID.randomUUID();
        ResourceView resource = new ResourceView(UUID.randomUUID(), ResourceType.BOOK, "移动端书籍", null,
            run.ikaros.resource.api.ResourceClassification.PRIVATE, ResourceLifecycle.ACTIVE, List.of(), List.of(),
            Instant.now(), Instant.now(), 0L);
        PageResponse<ResourceView> page = new PageResponse<>(List.of(resource), 1, 1, 20);
        when(service.list(ownerId, ResourceType.BOOK, "书", ResourceLifecycle.ACTIVE, 1, 20))
            .thenReturn(Mono.just(page));

        StepVerifier.create(controller.list(ownerId, ResourceType.BOOK, "书", ResourceLifecycle.ACTIVE, 1, 20))
            .assertNext(result -> assertThat(result).isEqualTo(page))
            .verifyComplete();

        verify(service).list(ownerId, ResourceType.BOOK, "书", ResourceLifecycle.ACTIVE, 1, 20);
    }

    @Test
    void returnsEmptyPageWithoutLeakingOtherOwners() {
        UUID ownerId = UUID.randomUUID();
        PageResponse<ResourceView> empty = new PageResponse<>(List.of(), 0, 0, 20);
        when(service.list(ownerId, null, null, ResourceLifecycle.ACTIVE, 0, 20)).thenReturn(Mono.just(empty));

        StepVerifier.create(controller.list(ownerId, null, null, ResourceLifecycle.ACTIVE, 0, 20))
            .assertNext(result -> assertThat(result.items()).isEmpty())
            .verifyComplete();

        verify(service).list(ownerId, null, null, ResourceLifecycle.ACTIVE, 0, 20);
    }
}
