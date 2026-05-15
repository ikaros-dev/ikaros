package run.ikaros.server.core.webclient;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentConst;
import run.ikaros.api.core.attachment.AttachmentUploadCondition;
import run.ikaros.server.core.attachment.service.AttachmentService;

@Slf4j
@Service
public class WeClientServiceImpl implements WeClientService {
    private final WebClient webClient;
    private final AttachmentService attachmentService;

    public WeClientServiceImpl(WebClient webClient, AttachmentService attachmentService) {
        this.webClient = webClient;
        this.attachmentService = attachmentService;
    }

    @NotNull
    @Override
    public Mono<Attachment> downloadImageWithGet(@NotBlank String policy,
                                                 @NotBlank String url) {
        Assert.hasText(policy, "'policy' must has text.");
        Assert.hasText(url, "'url' must has text.");
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
        return webClient.get().uri(URI.create(url))
            .exchangeToMono(clientResponse -> clientResponse.bodyToMono(byte[].class))
            .map(dataBufferFactory::wrap)
            .flatMap(dataBuffer -> Mono.just(Mono.just(dataBuffer).flux()))
            .flatMap(dataBuffer -> attachmentService.upload(AttachmentUploadCondition.builder()
                .dataBufferFlux(dataBuffer)
                .name(fileName)
                .parentId(AttachmentConst.DOWNLOAD_DIRECTORY_ID)
                .build()));
    }
}
