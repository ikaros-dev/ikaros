package run.ikaros.server.core.file;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.entity.FileEntity;

public interface FileService {
    Mono<Void> receiveAndHandleFragmentUploadChunkFile(@NotBlank String unique,
                                                       @Nonnull Long uploadLength,
                                                       @Nonnull Long uploadOffset,
                                                       @NotBlank String uploadName,
                                                       byte[] bytes);

    Mono<Void> revertFragmentUploadFile(@NotBlank String unique);

    Mono<FileEntity> updateEntity(FileEntity fileEntity);

    Mono<List<FileEntity>> listEntitiesByCondition(@Nonnull FindFileCondition findFileCondition);
}
