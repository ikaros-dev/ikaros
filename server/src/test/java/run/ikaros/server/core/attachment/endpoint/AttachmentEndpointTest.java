package run.ikaros.server.core.attachment.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.server.core.attachment.service.AttachmentService;

/**
 * {@link AttachmentEndpoint} 流接口单元测试.
 *
 * <p>验证 redirect 场景下返回 307 临时重定向（保留请求方法与 Range 语义），
 * 非外部直链与 redirect=false 时回退到服务端流式代理.</p>
 *
 * @author Nekoli
 */
class AttachmentEndpointTest {

    private AttachmentService attachmentService;
    private AttachmentEndpoint endpoint;

    @BeforeEach
    void setUp() {
        attachmentService = mock(AttachmentService.class);
        endpoint = new AttachmentEndpoint(attachmentService);
    }

    @Test
    void getStreamById_redirectEnabled_externalUrlReturns307() throws Exception {
        UUID id = UUID.randomUUID();
        String externalUrl = "https://s3.example.com/movies/1.mp4?X-Amz-Signature=abc";
        when(attachmentService.getReadUrl(id)).thenReturn(Mono.just(externalUrl));

        ServerResponse response = invokeGetStreamById(id, Optional.empty()).block();

        assertThat(response.statusCode()).isEqualTo(HttpStatus.TEMPORARY_REDIRECT);
        assertThat(response.headers().getFirst(HttpHeaders.LOCATION)).isEqualTo(externalUrl);
    }

    @Test
    void getStreamById_redirectEnabled_relativeUrlFallsBackToProxy() throws Exception {
        UUID id = UUID.randomUUID();
        when(attachmentService.getReadUrl(id))
            .thenReturn(Mono.just("/api/attachment/stream/id/" + id));
        Attachment att = Attachment.builder().id(id).name("movie.mp4").size(1024L).build();
        when(attachmentService.findById(id)).thenReturn(Mono.just(att));
        when(attachmentService.getStreamByIdWithoutRange(id))
            .thenReturn(Mono.just(Flux.<DataBuffer>empty()));

        ServerResponse response = invokeGetStreamById(id, Optional.of("true")).block();

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getStreamById_redirectDisabled_fallsBackToProxy() throws Exception {
        UUID id = UUID.randomUUID();
        Attachment att = Attachment.builder().id(id).name("movie.mp4").size(1024L).build();
        when(attachmentService.findById(id)).thenReturn(Mono.just(att));
        when(attachmentService.getStreamByIdWithoutRange(id))
            .thenReturn(Mono.just(Flux.<DataBuffer>empty()));

        ServerResponse response = invokeGetStreamById(id, Optional.of("false")).block();

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
    }

    @SuppressWarnings("unchecked")
    private Mono<ServerResponse> invokeGetStreamById(UUID id, Optional<String> redirect)
        throws Exception {
        ServerRequest request = mock(ServerRequest.class);
        when(request.pathVariable("id")).thenReturn(id.toString());
        when(request.queryParam("redirect")).thenReturn(redirect);
        ServerRequest.Headers headers = mock(ServerRequest.Headers.class);
        when(request.headers()).thenReturn(headers);
        when(headers.firstHeader(HttpHeaders.RANGE)).thenReturn(null);
        var method = AttachmentEndpoint.class.getDeclaredMethod(
            "getStreamById", ServerRequest.class);
        method.setAccessible(true);
        return (Mono<ServerResponse>) method.invoke(endpoint, request);
    }
}