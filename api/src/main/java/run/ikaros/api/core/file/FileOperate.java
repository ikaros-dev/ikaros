package run.ikaros.api.core.file;

import jakarta.validation.constraints.NotBlank;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;
import run.ikaros.api.store.enums.FileType;

public interface FileOperate extends AllowPluginOperate {

    Mono<Boolean> existsById(Long id);

    Mono<Boolean> existsByOriginalPath(@NotBlank String originalPath);

    Mono<File> findByOriginalPath(@NotBlank String originalPath);

    Mono<File> findById(Long id);

    Flux<File> findAllByOriginalNameLikeAndType(String name, FileType type);

    Mono<File> create(File entity);

    Mono<File> update(File entity);

    Mono<Void> deleteById(Long id);

    Mono<File> upload(String fileName, Flux<DataBuffer> dataBufferFlux);
}
