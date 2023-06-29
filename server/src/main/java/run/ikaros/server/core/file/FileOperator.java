package run.ikaros.server.core.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.file.File;
import run.ikaros.api.core.file.FileOperate;
import run.ikaros.api.store.entity.FileEntity;
import run.ikaros.api.store.enums.FileType;
import run.ikaros.server.infra.utils.ReactiveBeanUtils;
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
    public Mono<Boolean> existsByOriginalPath(String originalPath) {
        Assert.hasText(originalPath, "'originalPath' must has text.");
        return repository.existsByOriginalPath(originalPath)
            .doOnSuccess(unused ->
                log.debug("find file entity exists  by original path:[{}].", originalPath));
    }

    @Override
    public Mono<FileEntity> findByOriginalPath(String originalPath) {
        Assert.hasText(originalPath, "'originalPath' must has text.");
        return repository.findByOriginalPath(originalPath)
            .doOnSuccess(unused ->
                log.debug("find file entity  by original path:[{}].", originalPath));
    }

    @Override
    public Mono<FileEntity> findById(Long id) {
        Assert.notNull(id, "'id' must not null.");
        return repository.findById(id);
    }

    @Override
    public Flux<FileEntity> findAllByOriginalNameLikeAndType(String originalName, FileType type) {
        Assert.hasText(originalName, "'originalName' must has text.");
        Assert.notNull(type, "'type' must not null.");
        String originalNameLike = "%" + originalName + "%";
        PageRequest pageRequest = PageRequest.of(0, 99999);
        return repository.findAllByOriginalNameLikeAndType(originalNameLike, type, pageRequest);
    }

    @Override
    public Mono<FileEntity> create(FileEntity entity) {
        Assert.notNull(entity, "'entity' must not null.");
        Assert.isNull(entity.getId(), "'entity id' must null when create.");
        return repository.save(entity)
            .doOnSuccess(unused -> log.debug("update file entity by newEntity:[{}].", entity));
    }

    @Override
    public Mono<FileEntity> update(FileEntity entity) {
        Assert.notNull(entity, "'entity' must not null.");
        Assert.notNull(entity.getId(), "'entity id' must not null when update.");
        return findById(entity.getId())
            .flatMap(ent -> ReactiveBeanUtils.copyProperties(entity, ent))
            .flatMap(repository::save)
            .doOnSuccess(unused -> log.debug("update file entity by newEntity:[{}].", entity));
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
}
