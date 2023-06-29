package run.ikaros.server.core.file;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.file.File;
import run.ikaros.api.store.entity.FileEntity;
import run.ikaros.api.wrap.PagingWrap;

public interface FileService {
    Mono<Void> receiveAndHandleFragmentUploadChunkFile(@NotBlank String unique,
                                                       @Nonnull Long uploadLength,
                                                       @Nonnull Long uploadOffset,
                                                       @NotBlank String uploadName,
                                                       byte[] bytes);

    Mono<Void> revertFragmentUploadFile(@NotBlank String unique);

    Mono<FileEntity> updateEntity(FileEntity fileEntity);

    Mono<PagingWrap<FileEntity>> listEntitiesByCondition(
        @Nonnull FindFileCondition findFileCondition);

    Flux<FileEntity> findAll();

    Mono<FileEntity> findById(Long id);

    Mono<Void> deleteById(Long id);

    Mono<FileEntity> save(FileEntity entity);

    Mono<File> upload(String fileName, Flux<DataBuffer> dataBufferFlux);

    Mono<Void> pushRemote(Long fileId, String remote);

    Mono<Void> pullRemote(Long fileId, String remote);
}
