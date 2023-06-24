package run.ikaros.api.core.file;

import jakarta.validation.constraints.NotBlank;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;
import run.ikaros.api.store.entity.FileEntity;
import run.ikaros.api.store.enums.FileType;

public interface FileOperate extends AllowPluginOperate {

    Mono<Boolean> existsById(Long id);

    Mono<Boolean> existsByOriginalPath(@NotBlank String originalPath);

    Mono<FileEntity> findByOriginalPath(@NotBlank String originalPath);

    Mono<FileEntity> findById(Long id);

    Flux<FileEntity> findAllByOriginalNameLikeAndType(String name, FileType type);

    Mono<FileEntity> create(FileEntity entity);

    Mono<FileEntity> update(FileEntity entity);

    Mono<Void> deleteById(Long id);

    Mono<File> upload(String fileName, Flux<DataBuffer> dataBufferFlux, String policy);
}
