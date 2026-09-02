package run.ikaros.relation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** 验证资源关系控制器的 HTTP 合约。 */
class ResourceRelationControllerTest {
    private ResourceRelationService service;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        service = mock(ResourceRelationService.class);
        client = WebTestClient.bindToController(new ResourceRelationController(service)).build();
    }

    @Test
    void exposesCreateListAndDeleteEndpoints() {
        UUID ownerId = UUID.randomUUID(); UUID sourceId = UUID.randomUUID(); UUID targetId = UUID.randomUUID();
        UUID relationId = UUID.randomUUID();
        ResourceRelationView view = new ResourceRelationView(relationId, targetId, ResourceRelationType.RELATED_TO, 0);
        when(service.create(any(), any(), any())).thenReturn(Mono.just(view));
        when(service.list(ownerId, sourceId)).thenReturn(Flux.just(view));
        when(service.remove(ownerId, sourceId, relationId)).thenReturn(Mono.empty());

        client.post().uri("/api/resources/{sourceId}/relations", sourceId).header("X-Ikaros-Actor-Id", ownerId.toString())
            .bodyValue(Map.of("targetResourceId", targetId.toString(), "type", "RELATED_TO", "position", 0))
            .exchange().expectStatus().isCreated();
        client.get().uri("/api/resources/{sourceId}/relations", sourceId).header("X-Ikaros-Actor-Id", ownerId.toString())
            .exchange().expectStatus().isOk();
        client.delete().uri("/api/resources/{sourceId}/relations/{relationId}", sourceId, relationId)
            .header("X-Ikaros-Actor-Id", ownerId.toString()).exchange().expectStatus().isNoContent();
        verify(service).remove(ownerId, sourceId, relationId);
    }
}
