package run.ikaros.storage;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import run.ikaros.common.PageResponse;

/** 验证身份级附件列表的条件分页入口。 */
class AttachmentControllerTest {
    private StorageService storageService;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        storageService = mock(StorageService.class);
        client = WebTestClient.bindToController(new AttachmentController(storageService,
            mock(DeliveryGrantService.class), mock(AttachmentPreviewService.class))).build();
    }

    @Test
    void listsAllAttachmentsWhenResourceFilterIsBlank() {
        UUID actorId = UUID.randomUUID();
        when(storageService.listPage(actorId, null, 2, 10))
            .thenReturn(Mono.just(new PageResponse<>(List.of(), 0, 2, 10)));

        client.get().uri("/api/attachments?resourceId=&page=2&size=10")
            .header("X-Ikaros-Actor-Id", actorId.toString())
            .exchange()
            .expectStatus().isOk();

        verify(storageService).listPage(eq(actorId), eq(null), eq(2), eq(10));
    }
}
