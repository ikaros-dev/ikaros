package run.ikaros.server.core.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.file.File;
import run.ikaros.api.core.file.FileOperate;
import run.ikaros.api.store.enums.FileType;
import run.ikaros.server.infra.utils.ReactiveBeanUtils;
import run.ikaros.server.store.entity.FileEntity;
import run.ikaros.server.store.repository.FileRepository;

@Slf4j
@Component
public class FileOperator implements FileOperate {
    private final FileRepository repository;
    private final FileService fileService;

    public FileOperator(FileRepository repository, FileService fileService) {
        this.repository = repository;
        this.fileService = fileService;
    }

    @Override
    public Mono<Boolean> existsById(Long id) {
        Assert.notNull(id, "'id' must not null.");
        return repository.existsById(id)
            .doOnSuccess(unused ->
                log.debug("find file entity exists  by id:[{}].", id));
    }

    @Override
    public Mono<File> findById(Long id) {
        Assert.notNull(id, "'id' must not null.");
        return repository.findById(id)
            .flatMap(fileEntity -> ReactiveBeanUtils.copyProperties(fileEntity, new File()));
    }

    @Override
    public Mono<File> create(File file) {
        Assert.notNull(file, "'file' must not null.");
        Assert.isNull(file.getId(), "'file id' must null when create.");
        return ReactiveBeanUtils.copyProperties(file, new FileEntity())
            .flatMap(repository::save)
            .flatMap(fileEntity -> ReactiveBeanUtils.copyProperties(fileEntity, new File()))
            .doOnSuccess(file1 -> log.debug("update file entity by newEntity:[{}].", file1));
    }

    @Override
    public Mono<File> update(File file) {
        Assert.notNull(file, "'file' must not null.");
        Assert.notNull(file.getId(), "'file id' must not null when update.");
        return findById(file.getId())
            .flatMap(ent -> ReactiveBeanUtils.copyProperties(ent, new FileEntity()))
            .flatMap(repository::save)
            .flatMap(fileEntity -> ReactiveBeanUtils.copyProperties(fileEntity, new File()))
            .doOnSuccess(file1 -> log.debug("update file entity by newEntity:[{}].", file1));
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        Assert.notNull(id, "'id' must not null.");
        return repository.deleteById(id)
            .doOnSuccess(unused -> log.debug("delete file entity by id:[{}].", id));
    }

    @Override
    public Mono<File> upload(String fileName, Flux<DataBuffer> dataBufferFlux) {
        return fileService.upload(fileName, dataBufferFlux);
    }

    @Override
    public Mono<Boolean> existsByFsPath(String fsPath) {
        Assert.hasText(fsPath, "'fsPath' must has text.");
        return repository.existsByFsPath(fsPath);
    }

    @Override
    public Flux<File> findAllByNameLikeAndType(String nameLike, FileType type) {
        return null;
    }
}
