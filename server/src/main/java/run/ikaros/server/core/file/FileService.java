package run.ikaros.server.core.file;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import reactor.core.publisher.Mono;

public interface FileService {
    Mono<Void> receiveAndHandleFragmentUploadChunkFile(@NotBlank String unique,
                                                       @Nonnull Long uploadLength,
                                                       @Nonnull Long uploadOffset,
                                                       @NotBlank String uploadName,
                                                       byte[] bytes);

    Mono<Void> revertFragmentUploadFile(@NotBlank String unique);
}
