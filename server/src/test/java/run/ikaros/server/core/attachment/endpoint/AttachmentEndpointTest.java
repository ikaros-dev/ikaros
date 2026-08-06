package run.ikaros.server.core.attachment.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.server.core.attachment.service.AttachmentMediaValidationService;
import run.ikaros.server.core.attachment.service.AttachmentService;

/** 验证附件流和隔离 SVG 预览接口的响应行为. */
class AttachmentEndpointTest {

    /** SVG 预览接口要求的固定内容安全策略. */
    private static final String SVG_CONTENT_SECURITY_POLICY =
        "sandbox; default-src 'none'; style-src 'unsafe-inline'; img-src data:";
    /** 模拟附件服务. */
    @Mock
    private AttachmentService attachmentService;
    @Mock
    private AttachmentMediaValidationService mediaValidationService;
    /** 用于调用附件函数式路由的测试客户端. */
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        AttachmentEndpoint endpoint = new AttachmentEndpoint(attachmentService,
            mediaValidationService);
        webTestClient = WebTestClient.bindToRouterFunction(endpoint.endpoint()).build();
    }

    @Test
    void getSvgPreviewById_returnsIsolatedSvgResponse() {
        UUID attachmentId = UUID.randomUUID();
        byte[] content = "<svg xmlns=\"http://www.w3.org/2000/svg\"/>"
            .getBytes(StandardCharsets.UTF_8);
        when(attachmentService.findById(attachmentId))
            .thenReturn(Mono.just(attachment(attachmentId, "cover.SvG", content.length)));
        when(attachmentService.getStreamByIdWithoutRange(attachmentId))
            .thenReturn(stream(content));

        webTestClient.get().uri("/attachment/svg-preview/id/{id}", attachmentId)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueEquals(HttpHeaders.CONTENT_TYPE, "image/svg+xml")
            .expectHeader().valueEquals("Content-Security-Policy",
                SVG_CONTENT_SECURITY_POLICY)
            .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
            .expectHeader().valueEquals(HttpHeaders.CONTENT_DISPOSITION, "inline")
            .expectBody(byte[].class).value(body -> assertThat(body).isEqualTo(content));

        verify(attachmentService).findById(attachmentId);
        verify(attachmentService).getStreamByIdWithoutRange(attachmentId);
    }

    @Test
    void getSvgPreviewById_returnsNotFoundForNonSvgAttachment() {
        UUID attachmentId = UUID.randomUUID();
        when(attachmentService.findById(attachmentId))
            .thenReturn(Mono.just(attachment(attachmentId, "cover.svg.png", 10)));

        webTestClient.get().uri("/attachment/svg-preview/id/{id}", attachmentId)
            .exchange()
            .expectStatus().isNotFound();

        verify(attachmentService).findById(attachmentId);
        verify(attachmentService, never()).getStreamByIdWithoutRange(attachmentId);
    }

    @Test
    void getSvgPreviewById_returnsNotFoundForMissingAttachment() {
        UUID attachmentId = UUID.randomUUID();
        when(attachmentService.findById(attachmentId)).thenReturn(Mono.empty());

        webTestClient.get().uri("/attachment/svg-preview/id/{id}", attachmentId)
            .exchange()
            .expectStatus().isNotFound();

        verify(attachmentService).findById(attachmentId);
        verify(attachmentService, never()).getStreamByIdWithoutRange(attachmentId);
    }

    @Test
    void getSvgPreviewById_keepsStreamReadFailure() {
        UUID attachmentId = UUID.randomUUID();
        when(attachmentService.findById(attachmentId))
            .thenReturn(Mono.just(attachment(attachmentId, "broken.svg", 10)));
        when(attachmentService.getStreamByIdWithoutRange(attachmentId))
            .thenReturn(Mono.error(new IllegalStateException("driver read failure")));

        webTestClient.get().uri("/attachment/svg-preview/id/{id}", attachmentId)
            .exchange()
            .expectStatus().is5xxServerError();

        verify(attachmentService).getStreamByIdWithoutRange(attachmentId);
    }

    @Test
    void getStreamById_keepsFullContentBehavior() {
        UUID attachmentId = UUID.randomUUID();
        byte[] content = "video-content".getBytes(StandardCharsets.UTF_8);
        when(attachmentService.findById(attachmentId))
            .thenReturn(Mono.just(attachment(attachmentId, "episode.mp4", content.length)));
        when(attachmentService.getStreamByIdWithoutRange(attachmentId))
            .thenReturn(stream(content));

        webTestClient.get().uri("/attachment/stream/id/{id}", attachmentId)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueEquals(HttpHeaders.CONTENT_TYPE, "video/mp4")
            .expectHeader().valueEquals(HttpHeaders.CONTENT_LENGTH,
                String.valueOf(content.length))
            .expectHeader().valueEquals(HttpHeaders.ACCEPT_RANGES, "bytes")
            .expectBody(byte[].class).value(body -> assertThat(body).isEqualTo(content));
    }

    @Test
    void getStreamById_keepsRangeRequestBehavior() {
        UUID attachmentId = UUID.randomUUID();
        byte[] content = "video".getBytes(StandardCharsets.UTF_8);
        when(attachmentService.findById(attachmentId))
            .thenReturn(Mono.just(attachment(attachmentId, "episode.mp4", 20)));
        when(attachmentService.getStreamByIdWithRange(attachmentId, 5, 9))
            .thenReturn(stream(content));

        webTestClient.get().uri("/attachment/stream/id/{id}", attachmentId)
            .header(HttpHeaders.RANGE, "bytes=5-9")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.PARTIAL_CONTENT)
            .expectHeader().valueEquals(HttpHeaders.CONTENT_TYPE, "video/mp4")
            .expectHeader().valueEquals(HttpHeaders.ACCEPT_RANGES, "bytes")
            .expectHeader().valueEquals(HttpHeaders.CONTENT_RANGE, "bytes 5-9/20")
            .expectHeader().valueEquals(HttpHeaders.CONTENT_LENGTH, "5")
            .expectBody(byte[].class).value(body -> assertThat(body).isEqualTo(content));
    }

    @Test
    void listMediaFormats_returnsServerPolicyHints() {
        webTestClient.get().uri("/attachment/media-formats")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$[*].format").exists();
    }

    private static Attachment attachment(UUID id, String name, long size) {
        return Attachment.builder().id(id).name(name).size(size).build();
    }

    private static Mono<Flux<DataBuffer>> stream(byte[] content) {
        DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(content);
        return Mono.just(Flux.just(buffer));
    }
}
