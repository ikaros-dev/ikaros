package run.ikaros.server.core.webclient;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import org.springframework.resilience.annotation.Retryable;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;

public interface WeClientService {
    /**
     * Download network image by url, and upload file system.
     *
     * @param url network image url
     * @return new file url in file system.
     */
    @Nonnull
    @Retryable
    Mono<Attachment> downloadImageWithGet(@NotBlank String policy,
                                          @NotBlank String url);
}
