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
import run.ikaros.server.core.file.FileService;
import run.ikaros.server.infra.utils.ReactiveBeanUtils;
import run.ikaros.server.store.entity.FileEntity;

@Slf4j
@Service
public class WeClientServiceImpl implements WeClientService {
    private final WebClient webClient;
    private final FileService fileService;

    public WeClientServiceImpl(WebClient webClient, FileService fileService) {
        this.webClient = webClient;
        this.fileService = fileService;
    }

    @NotNull
    @Override
    public Mono<FileEntity> downloadImageWithGet(@NotBlank String policy,
                                                 @NotBlank String url) {
        Assert.hasText(policy, "'policy' must has text.");
        Assert.hasText(url, "'url' must has text.");
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
        return webClient.get().uri(URI.create(url))
            .exchangeToMono(clientResponse -> clientResponse.bodyToMono(byte[].class))
            .map(dataBufferFactory::wrap)
            .flatMap(dataBuffer -> Mono.just(Mono.just(dataBuffer).flux()))
            .flatMap(dataBuffer -> fileService.upload(fileName, dataBuffer)
                .flatMap(file -> ReactiveBeanUtils.copyProperties(file, new FileEntity())));
    }
}
