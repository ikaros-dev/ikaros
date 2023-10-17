package run.ikaros.server.core.attachment.service.impl;

import static run.ikaros.server.infra.utils.ReactiveBeanUtils.copyProperties;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentSearchCondition;
import run.ikaros.api.core.attachment.AttachmentUploadCondition;
import run.ikaros.api.core.attachment.exception.AttachmentRemoveException;
import run.ikaros.api.core.attachment.exception.AttachmentUploadException;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.api.infra.utils.SystemVarUtils;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.repository.AttachmentRepository;

@Slf4j
@Service
public class AttachmentServiceImpl implements AttachmentService {
    private final AttachmentRepository repository;
    private final R2dbcEntityTemplate template;
    private final IkarosProperties ikarosProperties;

    /**
     * Construct.
     */
    public AttachmentServiceImpl(AttachmentRepository repository,
                                 R2dbcEntityTemplate template, IkarosProperties ikarosProperties) {
        this.repository = repository;
        this.template = template;
        this.ikarosProperties = ikarosProperties;
    }

    @Override
    public Mono<AttachmentEntity> saveEntity(AttachmentEntity attachmentEntity) {
        Assert.notNull(attachmentEntity, "'attachmentEntity' must not be null.");
        return repository.save(attachmentEntity.setUpdateTime(LocalDateTime.now()));
    }

    @Override
    public Mono<Attachment> save(Attachment attachment) {
        Assert.notNull(attachment, "'attachment' must not be null.");
        return repository.findById(attachment.getId())
            .flatMap(attachmentEntity -> copyProperties(attachment, attachmentEntity))
            .flatMap(this::saveEntity)
            .flatMap(attachmentEntity -> copyProperties(attachmentEntity, attachment));
    }

    @Override
    public Mono<PagingWrap<AttachmentEntity>> listEntitiesByCondition(
        AttachmentSearchCondition searchCondition) {
        Assert.notNull(searchCondition, "'condition' must no null.");

        final int page = Optional.ofNullable(searchCondition.getPage()).orElse(1);
        final int size = Optional.ofNullable(searchCondition.getSize()).orElse(10);

        final String name = StringUtils.hasText(searchCondition.getName())
            ? searchCondition.getName() : "";
        final String nameLike = "%" + name + "%";
        final AttachmentType type = searchCondition.getType();
        final Long parentId = searchCondition.getParentId();

        PageRequest pageRequest = PageRequest.of(page - 1, size);

        Criteria criteria = Objects.isNull(parentId)
            ? Criteria.where("parent_id").isNull()
            : Criteria.where("parent_id").is(parentId);

        if (Objects.nonNull(type)) {
            criteria = criteria.and("type").is(type);
        }

        if (!StringUtils.hasText(name)) {
            criteria = criteria.and("name").like(nameLike);
        }

        Query query = Query.query(criteria).with(pageRequest);

        Flux<AttachmentEntity> attachmentEntityFlux =
            template.select(query, AttachmentEntity.class);
        Mono<Long> countMono = template.count(query, AttachmentEntity.class);

        return countMono.flatMap(total -> attachmentEntityFlux.collectList()
            .map(attachmentEntities -> new PagingWrap<>(page, size, total, attachmentEntities)));
    }

    @Override
    public Mono<PagingWrap<Attachment>> listByCondition(AttachmentSearchCondition searchCondition) {
        Assert.notNull(searchCondition, "'condition' must no null.");
        return listEntitiesByCondition(searchCondition)
            .flatMap(pagingWrap -> Flux.fromStream(pagingWrap.getItems().stream())
                .flatMap(attachmentEntity -> copyProperties(attachmentEntity, new Attachment()))
                .collectList()
                .map(attachments -> new PagingWrap<>(pagingWrap.getPage(), pagingWrap.getSize(),
                    pagingWrap.getTotal(), attachments)));
    }

    @Override
    public Mono<Void> removeById(Long attachmentId) {
        Assert.isTrue(attachmentId > 0, "'attachmentId' must gt 0.");
        return repository.findById(attachmentId)
            .map(this::removeFileSystemFile)
            .flatMap(repository::delete);
    }

    @Override
    public Mono<Attachment> upload(AttachmentUploadCondition uploadCondition) {
        Assert.notNull(uploadCondition, "'uploadCondition' must not null.");
        String name = uploadCondition.getName();
        final Boolean isAutoReName =
            Optional.ofNullable(uploadCondition.getIsAutoReName()).orElse(true);
        Long parentId = uploadCondition.getParentId();

        // build file upload path
        String uploadFilePath =
            FileUtils.buildAppUploadFilePath(ikarosProperties.getWorkDir().toString(),
                FileUtils.parseFilePostfix(name));

        // upload file data buffer
        return writeDataToFsPath(uploadCondition.getDataBufferFlux(), Path.of(uploadFilePath))
            .publishOn(Schedulers.boundedElastic())
            .flatMap(fsPath ->
                // rename if isAutoReName=true and exists same file.
                repository.existsByTypeAndParentIdAndName(
                        AttachmentType.File, parentId, name
                    )
                    .filter(exists -> isAutoReName && exists)
                    .map(exists -> System.currentTimeMillis() + "-" + name)
                    .switchIfEmpty(Mono.just(name))
                    .flatMap(n ->
                        // save attachment entity
                        saveEntity(AttachmentEntity.builder()
                            .fsPath(fsPath.toString())
                            .updateTime(LocalDateTime.now())
                            .type(AttachmentType.File)
                            .name(n)
                            .url(path2url(uploadFilePath, ikarosProperties.getWorkDir().toString()))
                            .size(findFileSize(uploadFilePath))
                            .build()))
            )
            .flatMap(attachmentEntity ->
                copyProperties(attachmentEntity, Attachment.builder().build()));
    }

    @Override
    public Mono<Attachment> findById(Long attachmentId) {
        Assert.isTrue(attachmentId > 0, "'attachmentId' must gt 0.");
        return repository.findById(attachmentId)
            .flatMap(attachmentEntity -> copyProperties(attachmentEntity, new Attachment()));
    }

    @Override
    public Mono<Attachment> findByTypeAndParentIdAndName(AttachmentType type,
                                                         @Nullable Long parentId, String name) {
        Assert.notNull(type, "'type' must not null.");
        Assert.hasText(name, "'name' must has text.");
        return repository.findByTypeAndParentIdAndName(type, parentId, name)
            .flatMap(attachmentEntity -> copyProperties(attachmentEntity, new Attachment()));
    }

    @Override
    public Mono<Void> removeByTypeAndParentIdAndName(AttachmentType type,
                                                     @Nullable Long parentId, String name) {
        Assert.notNull(type, "'type' must not null.");
        Assert.hasText(name, "'name' must has text.");
        return repository.removeByTypeAndParentIdAndName(type, parentId, name);
    }

    private static Mono<Path> writeDataToFsPath(Flux<DataBuffer> dataBufferFlux,
                                                Path fsPath) {
        File file = fsPath.toFile();
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new AttachmentUploadException(
                    "Attachment upload fail for file system path: " + fsPath, e);
            }
        }
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        FileOutputStream finalFileOutputStream = fileOutputStream;
        return DataBufferUtils.write(dataBufferFlux, fileOutputStream)
            .publishOn(Schedulers.boundedElastic())
            .doFinally(signalType -> {
                try {
                    finalFileOutputStream.close();
                } catch (IOException e) {
                    throw new AttachmentUploadException(
                        "Attachment upload fail for file system path: " + fsPath, e);
                }
            })
            .then(Mono.just(fsPath));
    }

    private String path2url(@NotBlank String path, @Nullable String workDir) {
        Assert.hasText(path, "'path' must has text.");
        String url = "";
        String currentAppDirPath =
            StringUtils.hasText(workDir) ? workDir : SystemVarUtils.getCurrentAppDirPath();
        url = path.replace(currentAppDirPath, "");
        // 如果是ntfs目录，则需要替换下 \ 为 /
        if (SystemVarUtils.platformIsWindows()) {
            url = url.replace("\\", "/");
        }
        log.debug("current url={}", url);
        return url;
    }

    private Long findFileSize(String uploadFilePath) {
        try {
            return Files.size(Path.of(uploadFilePath));
        } catch (IOException e) {
            log.warn("Get file size fail for file system path: {}", uploadFilePath, e);
            return 0L;
        }
    }

    @Override
    public Mono<Void> receiveAndHandleFragmentUploadChunkFile(String unique,
                                                              @Nonnull Long uploadLength,
                                                              @Nonnull Long uploadOffset,
                                                              String uploadName, byte[] bytes) {
        // todo impl
        return null;
    }

    @Override
    public Mono<Void> revertFragmentUploadFile(String unique) {
        // todo impl
        return null;
    }

    private AttachmentEntity removeFileSystemFile(AttachmentEntity attachmentEntity) {
        String fsPath = attachmentEntity.getFsPath();
        try {
            Files.deleteIfExists(Path.of(fsPath));
        } catch (IOException e) {
            throw new AttachmentRemoveException(
                "Attachment delete fail for file system path：" + fsPath, e);
        }
        return attachmentEntity;
    }
}
