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
    public Mono<Boolean> existsByOriginalPath(String originalPath) {
        Assert.hasText(originalPath, "'originalPath' must has text.");
        return repository.existsByOriginalPath(originalPath)
            .doOnSuccess(unused ->
                log.debug("find file entity exists  by original path:[{}].", originalPath));
    }

    @Override
    public Mono<File> findByOriginalPath(String originalPath) {
        Assert.hasText(originalPath, "'originalPath' must has text.");
        return repository.findByOriginalPath(originalPath)
            .flatMap(fileEntity -> ReactiveBeanUtils.copyProperties(fileEntity, new File()))
            .doOnSuccess(unused ->
                log.debug("find file entity  by original path:[{}].", originalPath));
    }

    @Override
    public Mono<File> findById(Long id) {
        Assert.notNull(id, "'id' must not null.");
        return repository.findById(id)
            .flatMap(fileEntity -> ReactiveBeanUtils.copyProperties(fileEntity, new File()));
    }

    @Override
    public Flux<File> findAllByOriginalNameLikeAndType(String originalName, FileType type) {
        Assert.hasText(originalName, "'originalName' must has text.");
        Assert.notNull(type, "'type' must not null.");
        String originalNameLike = "%" + originalName + "%";
        PageRequest pageRequest = PageRequest.of(0, 99999);
        return repository.findAllByOriginalNameLikeAndType(originalNameLike, type, pageRequest)
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
}
