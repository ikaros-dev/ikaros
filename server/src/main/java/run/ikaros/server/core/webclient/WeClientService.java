package run.ikaros.server.core.webclient;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.NonNull;
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
    @NonNull
    @Retryable
    Mono<Attachment> downloadImageWithGet(@NotBlank String policy,
                                          @NotBlank String url);
}
