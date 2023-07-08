package run.ikaros.api.core.file;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;
import run.ikaros.api.store.enums.FileType;

public interface FileOperate extends AllowPluginOperate {

    Mono<Boolean> existsById(Long id);

    Mono<File> findById(Long id);

    Mono<File> create(File entity);

    Mono<File> update(File entity);

    Mono<Void> deleteById(Long id);

    Mono<File> upload(String fileName, Flux<DataBuffer> dataBufferFlux);

    @Deprecated
    Mono<Boolean> existsByFsPath(String fsPath);

    Mono<Boolean> existsByMd5(String md5);

    Flux<File> findAllByNameLikeAndType(String nameLike, FileType type);

    Mono<File> updateFolder(Long id, Long folderId);
}
