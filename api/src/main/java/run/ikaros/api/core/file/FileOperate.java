package run.ikaros.api.core.file;

import jakarta.validation.constraints.NotBlank;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;
import run.ikaros.api.store.entity.FileEntity;

public interface FileOperate extends AllowPluginOperate {

    Mono<Boolean> existsById(Long id);

    Mono<Boolean> existsByOriginalPath(@NotBlank String originalPath);

    Mono<FileEntity> findByOriginalPath(@NotBlank String originalPath);

    Mono<FileEntity> findById(Long id);

    Mono<FileEntity> create(FileEntity entity);

    Mono<FileEntity> update(FileEntity entity);

    Mono<Void> deleteById(Long id);
}
