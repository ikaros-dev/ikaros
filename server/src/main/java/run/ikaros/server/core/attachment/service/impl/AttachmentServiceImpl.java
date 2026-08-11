package run.ikaros.server.core.attachment.service.impl;

import static org.springframework.util.FileCopyUtils.BUFFER_SIZE;
import static run.ikaros.api.core.attachment.AttachmentConst.ROOT_DIRECTORY_ID;
import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;
import static run.ikaros.api.store.enums.AttachmentType.Directory;
import static run.ikaros.api.store.enums.AttachmentType.Driver_Directory;
import static run.ikaros.api.store.enums.AttachmentType.Driver_File;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.attachment.AccessUrlCondition;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentAccessUrlProvider;
import run.ikaros.api.core.attachment.AttachmentConst;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.core.attachment.AttachmentSearchCondition;
import run.ikaros.api.core.attachment.AttachmentStreamVo;
import run.ikaros.api.core.attachment.AttachmentUploadCondition;
import run.ikaros.api.core.attachment.exception.AttachmentParentNotFoundException;
import run.ikaros.api.core.attachment.exception.AttachmentRemoveException;
import run.ikaros.api.core.attachment.exception.AttachmentUploadException;
import run.ikaros.api.core.attachment.exception.NoAvailableAttDriverFetcherException;
import run.ikaros.api.core.media.MediaFileDetectionResult;
import run.ikaros.api.core.media.MediaFileFormat;
import run.ikaros.api.core.media.MediaFilePolicy;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.api.infra.utils.SystemVarUtils;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.cache.annotation.MonoCacheEvict;
import run.ikaros.server.cache.annotation.MonoCacheable;
import run.ikaros.server.core.attachment.event.AttachmentRemoveEvent;
import run.ikaros.server.core.attachment.service.AttachmentMediaValidationService;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.plugin.ExtensionComponentsFinder;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.repository.AttachmentDriverRepository;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;
import run.ikaros.server.store.repository.AttachmentRelationRepository;
import run.ikaros.server.store.repository.AttachmentRepository;

@Slf4j
@Service
public class AttachmentServiceImpl implements AttachmentService {
    private final AttachmentRepository repository;
    private final AttachmentReferenceRepository referenceRepository;
    private final AttachmentRelationRepository relationRepository;
    private final R2dbcEntityTemplate template;
    private final IkarosProperties ikarosProperties;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentDriverRepository driverRepository;
    private final ExtensionComponentsFinder extensionComponentsFinder;
    /**
     * 附件名称门禁和有限前缀真实格式验证服务.
     */
    private final AttachmentMediaValidationService mediaValidationService;

    /**
     * Construct.
     */
    public AttachmentServiceImpl(AttachmentRepository repository,
                                 AttachmentReferenceRepository referenceRepository,
                                 AttachmentRelationRepository relationRepository,
                                 R2dbcEntityTemplate template, IkarosProperties ikarosProperties,
                                 ApplicationEventPublisher applicationEventPublisher,
                                 AttachmentRepository attachmentRepository,
                                 AttachmentDriverRepository driverRepository,
                                 ExtensionComponentsFinder extensionComponentsFinder,
                                 AttachmentMediaValidationService mediaValidationService) {
        this.repository = repository;
        this.referenceRepository = referenceRepository;
        this.relationRepository = relationRepository;
        this.template = template;
        this.ikarosProperties = ikarosProperties;
        this.applicationEventPublisher = applicationEventPublisher;
        this.attachmentRepository = attachmentRepository;
        this.driverRepository = driverRepository;
        this.extensionComponentsFinder = extensionComponentsFinder;
        this.mediaValidationService = mediaValidationService;
    }

    @Override
    @MonoCacheEvict
    public Mono<AttachmentEntity> saveEntity(AttachmentEntity attachmentEntity) {
        Assert.notNull(attachmentEntity, "'attachmentEntity' must not be null.");
        return repository
            .findByTypeAndParentIdAndName(attachmentEntity.getType(),
                attachmentEntity.getParentId(), attachmentEntity.getName())
            .switchIfEmpty(findPathByParentId(
                attachmentEntity.getParentId(),
                attachmentEntity.getName())
                .map(attachmentEntity::setPath))
            .flatMap(entity ->
                copyProperties(attachmentEntity, entity, "path"))
            .map(entity -> entity.setUpdateTime(LocalDateTime.now()))
            .flatMap(entity -> {
                if (entity.getId() == null) {
                    entity.setId(UuidV7Utils.generateUuid());
                    return attachmentRepository.insert(entity);
                } else {
                    return attachmentRepository.update(entity);
                }
            });
    }

    @Override
    @MonoCacheEvict
    public Mono<Attachment> save(Attachment attachment) {
        Assert.notNull(attachment, "'attachment' must not be null.");
        String fsPath = attachment.getFsPath();
        if (StringUtils.hasText(fsPath) && !fsPath.startsWith("http")) {
            if (isDriverAttachment(attachment)) {
                Assert.notNull(attachment.getDriverId(),
                    "'driverId' must not be null for driver attachment.");
            } else {
                validateFsPath(fsPath);
            }
        }
        attachment.setParentId(Optional
            .ofNullable(attachment.getParentId())
            .orElse(AttachmentConst.ROOT_DIRECTORY_ID));
        final UUID newParentId = attachment.getParentId();
        Mono<Void> targetDirectoryValidation = AttachmentType.File.equals(attachment.getType())
            ? validateFileTargetDirectory(newParentId)
            : Mono.empty();
        return targetDirectoryValidation
            .then(Mono.defer(() -> Objects.isNull(attachment.getId())
                ? copyProperties(attachment, new AttachmentEntity())
                : repository
                .findById(attachment.getId())
                .flatMap(attachmentEntity ->
                    copyProperties(attachment, attachmentEntity, "parentId"))))
            .flatMap(attachmentEntity -> updatePathWhenNewParentId(attachmentEntity, newParentId))
            .flatMap(this::saveEntity)
            .flatMap(attachmentEntity -> copyProperties(attachmentEntity, attachment));
    }

    private boolean isDriverAttachment(Attachment attachment) {
        return Driver_File.equals(attachment.getType())
            || Driver_Directory.equals(attachment.getType());
    }

    private Mono<AttachmentEntity> updatePathWhenNewParentId(AttachmentEntity attachmentEntity,
                                                             UUID newParentId) {
        if (Objects.equals(newParentId, attachmentEntity.getParentId())) {
            return Mono.just(attachmentEntity);
        }

        String name = attachmentEntity.getName();
        return findPathByParentId(newParentId, name)
            .map(attachmentEntity::setPath)
            .map(attEntity -> attEntity.setParentId(newParentId));
    }

    @Override
    @MonoCacheable(value = "attachment:entities:", key = "#searchCondition.toString()")
    public Mono<PagingWrap<AttachmentEntity>> listEntitiesByCondition(
        AttachmentSearchCondition searchCondition) {
        Assert.notNull(searchCondition, "'condition' must no null.");

        final int page = Optional
            .ofNullable(searchCondition.getPage())
            .orElse(1);
        final int size = Optional
            .ofNullable(searchCondition.getSize())
            .orElse(10);

        String[] nameKeyWords = StringUtils.hasText(searchCondition.getName())
            ? searchCondition
            .getName()
            .split(" ")
            : new String[] {};
        final AttachmentType type = searchCondition.getType();
        final UUID parentId = searchCondition.getParentId();
        final PageRequest pageRequest = PageRequest.of(page - 1, size);

        Criteria criteria = Criteria.empty();

        if (Objects.nonNull(parentId)) {
            criteria = Criteria
                .where("parent_id")
                .is(parentId);
        }

        if (Objects.nonNull(type)) {
            criteria = criteria
                .and("type")
                .is(type);
        }

        for (String nameKeyWord : nameKeyWords) {
            if (!StringUtils.hasText(nameKeyWord)) {
                continue;
            }
            String nameKeyWordLike = "%" + nameKeyWord + "%";
            criteria = criteria
                .and("name")
                .like(nameKeyWordLike);
        }

        criteria = criteria.and(Criteria
            .where("deleted")
            .is(false)
            .or(Criteria
                .where("deleted")
                .isNull()));


        Query query = Query
            .query(criteria)
            .sort(Sort.by(Sort.Order.asc("type")))
            .sort(Sort.by(Sort.Order.asc("name")))
            .sort(Sort.by(Sort.Order.asc("size")))
            .sort(Sort.by(Sort.Order.asc("update_time")))
            .with(pageRequest);

        Flux<AttachmentEntity> attachmentEntityFlux =
            template.select(query, AttachmentEntity.class);
        Mono<Long> countMono = template.count(query, AttachmentEntity.class);

        return countMono.flatMap(total -> attachmentEntityFlux
            .flatMap(attEntity -> findPathByParentId(attEntity.getParentId(), attEntity.getName())
                .filter(newPath -> !newPath.equals(attEntity.getPath()))
                .map(attEntity::setPath)
                .flatMap(attachmentRepository::update)
                .switchIfEmpty(Mono.just(attEntity)))
            .collectList()
            .map(attachmentEntities -> new PagingWrap<>(page, size, total, attachmentEntities)));
    }

    @Override
    @MonoCacheable(value = "attachments:", key = "#searchCondition.toString()")
    public Mono<PagingWrap<Attachment>> listByCondition(AttachmentSearchCondition searchCondition) {
        Assert.notNull(searchCondition, "'condition' must no null.");
        return listEntitiesByCondition(searchCondition)
            .flatMap(pagingWrap -> Flux
                .fromStream(pagingWrap
                    .getItems()
                    .stream())
                .flatMap(attachmentEntity -> copyProperties(attachmentEntity, new Attachment()))
                .collectList()
                .map(attachments -> new PagingWrap<>(pagingWrap.getPage(), pagingWrap.getSize(),
                    pagingWrap.getTotal(), attachments)));
    }


    private Mono<AttachmentEntity> checkChildAttachmentRefNotExists(
        AttachmentEntity attachmentEntity) {
        return Mono
            .just(attachmentEntity)
            .map(AttachmentEntity::getType)
            .filter(Directory::equals)
            .map(eq -> attachmentEntity.getId())
            .flatMapMany(repository::findAllByParentId)
            .flatMap(this::checkChildAttachmentRefNotExists)
            .flatMap(entity -> referenceRepository
                .existsByAttachmentId(entity.getId())
                .filter(exists -> !exists)
                .switchIfEmpty(Mono.error(new AttachmentRemoveException(
                    "Attachment references exists, "
                        +
                        "please remove all references for current attachment before remove it, id="
                        + entity.getId() + " and name=" + entity.getName()))))
            .then(Mono.just(attachmentEntity));
    }

    private Mono<UUID> checkAttachmentRefNotExists(UUID attachmentId) {
        return repository
            .findById(attachmentId)
            .flatMap(this::checkChildAttachmentRefNotExists)
            .flatMap(entity -> referenceRepository
                .existsByAttachmentId(entity.getId())
                .filter(exists -> !exists)
                .switchIfEmpty(Mono.error(new AttachmentRemoveException(
                    "Attachment references exists, "
                        +
                        "please remove all references for current attachment before remove it, id="
                        + entity.getId() + " and name=" + entity.getName()))))
            .then(Mono.just(attachmentId));
    }

    private Mono<AttachmentEntity> checkChildAttachmentRelNotExists(
        AttachmentEntity attachmentEntity) {
        return Mono
            .just(attachmentEntity)
            .map(AttachmentEntity::getType)
            .filter(Directory::equals)
            .map(eq -> attachmentEntity.getId())
            .flatMapMany(repository::findAllByParentId)
            .flatMap(this::checkChildAttachmentRelNotExists)
            .flatMap(entity -> relationRepository
                .existsByAttachmentId(entity.getId())
                .filter(exists -> !exists)
                .switchIfEmpty(Mono.error(new AttachmentRemoveException(
                    "Attachment relations exists, "
                        + "please remove all relations for current attachment before remove it, id="
                        + entity.getId() + " and name=" + entity.getName()))))
            .then(Mono.just(attachmentEntity));
    }

    private Mono<UUID> checkAttachmentRelNotExists(UUID attachmentId) {
        return repository
            .findById(attachmentId)
            .flatMap(this::checkChildAttachmentRelNotExists)
            .flatMap(entity -> relationRepository
                .existsByAttachmentId(entity.getId())
                .filter(exists -> !exists)
                .switchIfEmpty(Mono.error(new AttachmentRemoveException(
                    "Attachment relations exists, "
                        + "please remove all relations for current attachment before remove it, id="
                        + entity.getId() + " and name=" + entity.getName()))))
            .then(Mono.just(attachmentId));
    }

    private Mono<AttachmentEntity> removeChildrenAttachment(AttachmentEntity attachmentEntity) {
        return Mono
            .just(attachmentEntity)
            .map(AttachmentEntity::getType)
            .filter(Directory::equals)
            .map(eq -> attachmentEntity.getId())
            .flatMap(this::checkAttachmentRefNotExists)
            .flatMapMany(repository::findAllByParentId)
            .flatMap(this::removeChildrenAttachment)
            .switchIfEmpty(Mono.just(attachmentEntity))
            .map(this::removeFileSystemFile)
            .flatMap(this::deleteEntity)
            .then(Mono.just(attachmentEntity));
    }

    private Mono<AttachmentEntity> removeChildrenAttachmentForcibly(
        AttachmentEntity attachmentEntity) {
        return Mono
            .just(attachmentEntity)
            .map(AttachmentEntity::getType)
            .filter(attachmentType -> Directory.equals(attachmentType)
                || Driver_Directory.equals(attachmentType))
            .map(eq -> attachmentEntity.getId())
            .flatMapMany(repository::findAllByParentId)
            .flatMap(this::removeChildrenAttachmentForcibly)
            .switchIfEmpty(Mono.just(attachmentEntity))
            .map(this::removeFileSystemFile)
            .flatMap(this::deleteEntity)
            .then(Mono.just(attachmentEntity));
    }

    private Mono<AttachmentEntity> removeChildrenAttachmentOnlyRecords(
        AttachmentEntity attachmentEntity) {
        return Mono
            .just(attachmentEntity)
            .map(AttachmentEntity::getType)
            .filter(attachmentType -> Directory.equals(attachmentType)
                || Driver_Directory.equals(attachmentType))
            .map(eq -> attachmentEntity.getId())
            .flatMapMany(repository::findAllByParentId)
            .flatMap(this::removeChildrenAttachmentOnlyRecords)
            .switchIfEmpty(Mono.just(attachmentEntity))
            .flatMap(this::deleteEntityWithLogic)
            .then(Mono.just(attachmentEntity));
    }


    private Mono<Void> deleteEntity(AttachmentEntity attachmentEntity) {
        return repository
            .delete(attachmentEntity)
            .doOnSuccess(unused -> {
                AttachmentRemoveEvent event = new AttachmentRemoveEvent(this, attachmentEntity);
                applicationEventPublisher.publishEvent(event);
                log.debug("publish AttachmentRemoveEvent for attachment entity: [{}]",
                    attachmentEntity);
            });
    }

    private Mono<Void> deleteEntityWithLogic(AttachmentEntity attachmentEntity) {
        return saveEntity(attachmentEntity.setDeleted(true)).then();
    }

    @Override
    @MonoCacheEvict
    public Mono<Void> removeById(UUID attachmentId) {
        Assert.notNull(attachmentId, "'attachmentId' must not null.");
        if (AttachmentConst.COVER_DIRECTORY_ID.equals(attachmentId)
            || AttachmentConst.DOWNLOAD_DIRECTORY_ID.equals(attachmentId)) {
            return Mono.error(new AttachmentRemoveException(
                "Forbid remove system internal 'Covers' or 'Downloads' dir for attachment id="
                    + attachmentId));
        }
        return checkAttachmentRefNotExists(attachmentId)
            .flatMap(repository::findById)
            .flatMap(this::removeChildrenAttachment)
            .map(AttachmentEntity::getId)
            .flatMap(this::removeByIdForcibly);
    }

    @Override
    @MonoCacheEvict
    public Mono<Void> removeByIdForcibly(UUID attachmentId) {
        Assert.notNull(attachmentId, "'attachmentId' must not null.");
        return repository
            .findById(attachmentId)
            .flatMap(this::removeChildrenAttachmentForcibly)
            .map(this::removeFileSystemFile)
            .flatMap(this::deleteEntity);
    }

    @Override
    @MonoCacheEvict
    public Mono<Void> removeByIdOnlyRecords(UUID attachmentId) {
        Assert.notNull(attachmentId, "'attachmentId' must not null.");
        return repository
            .findById(attachmentId)
            .flatMap(this::removeChildrenAttachmentOnlyRecords)
            .flatMap(this::deleteEntityWithLogic);
    }


    @Override
    @MonoCacheEvict
    public Mono<Void> removeByTypeAndParentIdAndName(AttachmentType type,
                                                     @Nullable UUID parentId, String name) {
        Assert.notNull(type, "'type' must not null.");
        Assert.hasText(name, "'name' must has text.");
        if (Objects.isNull(parentId)) {
            parentId = AttachmentConst.ROOT_DIRECTORY_ID;
        }
        return repository
            .findByTypeAndParentIdAndName(type, parentId, name)
            .map(AttachmentEntity::getId)
            .flatMap(this::removeById);
    }

    @Override
    @MonoCacheEvict
    public Mono<Attachment> upload(AttachmentUploadCondition uploadCondition) {
        Assert.notNull(uploadCondition, "'uploadCondition' must not null.");
        String name = uploadCondition.getName();
        mediaValidationService.validateFilename(name);
        final Boolean isAutoReName =
            Optional
                .ofNullable(uploadCondition.getIsAutoReName())
                .orElse(true);
        UUID parentId = Optional
            .ofNullable(uploadCondition.getParentId())
            .orElse(AttachmentConst.ROOT_DIRECTORY_ID);
        AtomicReference<Path> targetPath = new AtomicReference<>();
        AtomicBoolean persisted = new AtomicBoolean();
        return validateFileTargetDirectory(parentId)
            .then(Mono.defer(() -> mediaValidationService
            .validate(uploadCondition.getDataBufferFlux(), name))
            .flatMap(validated -> Mono.defer(() -> {
                Path path = Path.of(FileUtils.buildAppUploadFilePath(
                    ikarosProperties
                        .getWorkDir()
                        .toString(),
                    MediaFilePolicy
                        .extractExtension(name)
                        .orElseThrow()));
                targetPath.set(path);
                return writeDataToFsPath(validated.content(), path);
            }))
            .flatMap(fsPath -> repository
                .existsByTypeAndParentIdAndName(
                    AttachmentType.File, parentId, name)
                .filter(exists -> isAutoReName && exists)
                .map(exists -> System.currentTimeMillis() + "-" + name)
                .switchIfEmpty(Mono.just(name))
                .flatMap(n -> findPathByParentId(parentId, n)
                    .map(path -> AttachmentEntity
                        .builder()
                        .parentId(parentId)
                        .fsPath(fsPath.toString())
                        .updateTime(LocalDateTime.now())
                        .type(AttachmentType.File)
                        .name(n)
                        .path(path)
                        .url(path2url(fsPath.toString(),
                            ikarosProperties
                                .getWorkDir()
                                .toString()))
                        .size(findFileSize(fsPath.toString()))
                        .build())
                    .flatMap(this::saveEntity)))
            .flatMap(attachmentEntity ->
                copyProperties(attachmentEntity, Attachment
                    .builder()
                    .build()))
            .doOnNext(attachment -> persisted.set(true))
            .doFinally(signalType -> {
                if (!persisted.get()) {
                    deleteFileQuietly(targetPath.get());
                }
            }));
    }

    @Override
    @MonoCacheable(value = "attachment:id:", key = "#attachmentId")
    public Mono<Attachment> findById(UUID attachmentId) {
        return repository
            .findById(attachmentId)
            .flatMap(attachmentEntity -> copyProperties(attachmentEntity, new Attachment()));
    }

    @Override
    public Mono<AttachmentEntity> findEntityById(UUID attachmentId) {
        return repository.findById(attachmentId);
    }

    @Override
    @MonoCacheable(value = "attachment:id:",
        key = "#type.toString() + ' ' + (#parentId?.toString() ?: '') + ' ' + #name")
    public Mono<Attachment> findByTypeAndParentIdAndName(AttachmentType type,
                                                         @Nullable UUID parentId, String name) {
        Assert.notNull(type, "'type' must not null.");
        Assert.hasText(name, "'name' must has text.");
        if (Objects.isNull(parentId)) {
            parentId = AttachmentConst.ROOT_DIRECTORY_ID;
        }
        return repository
            .findByTypeAndParentIdAndName(type, parentId, name)
            .flatMap(attachmentEntity -> copyProperties(attachmentEntity, new Attachment()));
    }


    private static Mono<Path> writeDataToFsPath(Flux<DataBuffer> dataBufferFlux,
                                                Path fsPath) {
        return Mono
            .fromCallable(() -> {
                Files.createDirectories(fsPath.getParent());
                return fsPath;
            })
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(path -> Mono.using(
                () -> Files.newOutputStream(path, StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE),
                outputStream -> DataBufferUtils
                    .write(dataBufferFlux, outputStream)
                    .doOnNext(buffer -> DataBufferUtils.release(buffer))
                    .doOnDiscard(DataBuffer.class, DataBufferUtils::release)
                    .then(Mono.just(path)), AttachmentServiceImpl::closeOutputStream));
    }

    private static void deleteFileQuietly(@Nullable Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.warn("清理未持久化附件失败: {}", path.getFileName(), exception);
        }
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

    private String findFileSha1(String uploadFilePath) {
        try {
            return FileUtils.calculateSha1(uploadFilePath);
        } catch (IOException | NoSuchAlgorithmException e) {
            log.warn("Get file sha1 fail for file system path: {}", uploadFilePath, e);
            return "";
        }
    }

    @Override
    @MonoCacheEvict
    public Mono<Void> receiveAndHandleFragmentUploadChunkFile(String unique,
                                                              @Nonnull Long uploadLength,
                                                              @Nonnull Long uploadOffset,
                                                              String uploadName,
                                                              Flux<DataBuffer> content,
                                                              @Nullable UUID parentId) {
        Assert.hasText(unique, "'unique' must has text.");
        Assert.notNull(uploadLength, "'uploadLength' must not null.");
        Assert.notNull(uploadOffset, "'uploadOffset' must not null.");
        Assert.hasText(uploadName, "'uploadName' must has text.");
        Assert.notNull(content, "'content' must not null.");
        if (uploadLength <= 0 || uploadOffset < 0 || uploadOffset >= uploadLength) {
            return content
                .doOnNext(DataBufferUtils::release)
                .then(Mono.error(
                    new IllegalArgumentException("无效的上传长度或分片偏移量")));
        }
        mediaValidationService.validateFilename(uploadName);
        Path sessionDir = fragmentSessionDir(unique);
        UUID resolvedParentId = Optional
            .ofNullable(parentId)
            .orElse(AttachmentConst.ROOT_DIRECTORY_ID);
        Mono<FragmentSession> sessionMono = uploadOffset == 0
            ? mediaValidationService
            .validate(content, uploadName)
            .flatMap(validated -> initializeFragmentSession(sessionDir, uploadName,
                uploadLength, validated.detectionResult(), validated.content()))
            : readFragmentSession(sessionDir)
            .flatMap(session -> validateFragmentSession(session, uploadName, uploadLength)
                .then(writeFragmentChunk(sessionDir, uploadOffset, content))
                .thenReturn(session));
        AtomicReference<Path> targetPath = new AtomicReference<>();
        return validateFileTargetDirectory(resolvedParentId)
            .then(sessionMono)
            .flatMap(session -> currentFragmentLength(sessionDir)
                .flatMap(currentLength -> {
                    if (currentLength > uploadLength) {
                        return Mono.error(new AttachmentUploadException(
                            "分片数据超过声明长度", null));
                    }
                    if (currentLength < uploadLength) {
                        return Mono.empty();
                    }
                    return completeFragmentUpload(sessionDir, session, resolvedParentId,
                        targetPath)
                        .then(cleanupFragmentUpload(sessionDir, null));
                }))
            .onErrorResume(throwable -> cleanupFragmentUpload(sessionDir, targetPath.get())
                .then(Mono.error(throwable)))
            .then();
    }

    private Mono<FragmentSession> initializeFragmentSession(
        Path sessionDir, String uploadName, long uploadLength,
        MediaFileDetectionResult detectionResult, Flux<DataBuffer> content) {
        FragmentSession session = new FragmentSession(uploadName, uploadLength,
            detectionResult.format());
        return cleanupFragmentUpload(sessionDir, null)
            .then(writeFragmentChunk(sessionDir, 0, content))
            .then(writeFragmentSession(sessionDir, session))
            .thenReturn(session);
    }

    private Mono<Void> validateFragmentSession(FragmentSession session, String uploadName,
                                               long uploadLength) {
        if (!session
            .uploadName()
            .equals(uploadName) || session.uploadLength() != uploadLength) {
            return Mono.error(new IllegalArgumentException("分片上传会话名称或长度不匹配"));
        }
        return Mono.empty();
    }

    private Mono<Long> writeFragmentChunk(Path sessionDir, long uploadOffset,
                                          Flux<DataBuffer> content) {
        return currentFragmentLength(sessionDir).flatMap(currentLength -> {
            if (currentLength != uploadOffset) {
                return Mono.error(new IllegalArgumentException("分片偏移量与已接收长度不匹配"));
            }
            Path chunkPath = sessionDir.resolve(Long.toString(uploadOffset));
            AtomicLong chunkLength = new AtomicLong();
            return Mono
                .fromCallable(() -> {
                    Files.createDirectories(sessionDir);
                    return chunkPath;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(path -> Mono.using(
                    () -> Files.newOutputStream(path, StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE),
                    outputStream -> DataBufferUtils
                        .write(
                            content.doOnNext(buffer -> chunkLength.addAndGet(
                                buffer.readableByteCount())), outputStream)
                        .doOnNext(buffer -> DataBufferUtils.release(buffer))
                        .doOnDiscard(DataBuffer.class, DataBufferUtils::release)
                        .then(Mono.defer(() -> chunkLength.get() == 0
                            ? Mono.error(new AttachmentUploadException("分片内容为空", null))
                            : Mono.just(chunkLength.get()))),
                    AttachmentServiceImpl::closeOutputStream));
        });
    }

    private static void closeOutputStream(OutputStream outputStream) {
        try {
            outputStream.close();
        } catch (IOException exception) {
            throw new AttachmentUploadException("关闭附件输出流失败", exception);
        }
    }

    private Mono<Void> writeFragmentSession(Path sessionDir, FragmentSession session) {
        return Mono
            .fromRunnable(() -> {
                Properties properties = new Properties();
                properties.setProperty("name", Base64
                    .getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(session
                        .uploadName()
                        .getBytes(StandardCharsets.UTF_8)));
                properties.setProperty("length", Long.toString(session.uploadLength()));
                properties.setProperty("format", session
                    .format()
                    .name());
                try (OutputStream outputStream = Files.newOutputStream(
                    sessionDir.resolve(".validated"), StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE)) {
                    properties.store(outputStream, null);
                } catch (IOException exception) {
                    throw new AttachmentUploadException("写入分片验证元数据失败", exception);
                }
            })
            .subscribeOn(Schedulers.boundedElastic())
            .then();
    }

    private Mono<FragmentSession> readFragmentSession(Path sessionDir) {
        return Mono
            .fromCallable(() -> {
                Path metadataPath = sessionDir.resolve(".validated");
                if (!Files.isRegularFile(metadataPath)) {
                    throw new IllegalArgumentException("分片上传会话尚未通过首分片验证");
                }
                Properties properties = new Properties();
                try (InputStream inputStream = Files.newInputStream(metadataPath)) {
                    properties.load(inputStream);
                }
                String uploadName = new String(Base64
                    .getUrlDecoder()
                    .decode(properties.getProperty("name")), StandardCharsets.UTF_8);
                long uploadLength = Long.parseLong(properties.getProperty("length"));
                MediaFileFormat format = MediaFileFormat.valueOf(
                    properties.getProperty("format"));
                return new FragmentSession(uploadName, uploadLength, format);
            })
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorMap(exception -> exception instanceof IOException
                    || exception instanceof NullPointerException
                    || exception instanceof IllegalArgumentException,
                exception -> exception instanceof IllegalArgumentException
                    && "分片上传会话尚未通过首分片验证".equals(exception.getMessage())
                    ? exception :
                    new IllegalArgumentException("分片上传会话元数据无效", exception));
    }

    private Mono<Long> currentFragmentLength(Path sessionDir) {
        return Mono
            .fromCallable(() -> {
                if (!Files.isDirectory(sessionDir)) {
                    return 0L;
                }
                try (var paths = Files.list(sessionDir)) {
                    return paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path
                            .getFileName()
                            .toString()
                            .matches("\\d+"))
                        .mapToLong(path -> {
                            try {
                                return Files.size(path);
                            } catch (IOException exception) {
                                throw new AttachmentUploadException(
                                    "读取分片长度失败", exception);
                            }
                        })
                        .sum();
                }
            })
            .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Void> completeFragmentUpload(Path sessionDir, FragmentSession session,
                                              UUID parentId,
                                              AtomicReference<Path> targetPath) {
        Path firstChunk = sessionDir.resolve("0");
        return mediaValidationService
            .validate(firstChunk, session.uploadName())
            .filter(result -> result.format() == session.format())
            .switchIfEmpty(Mono.error(new AttachmentUploadException(
                "最终合并前的真实格式检测不一致", null)))
            .flatMap(result -> Mono.defer(() -> {
                Path target = Path.of(FileUtils.buildAppUploadFilePath(
                    ikarosProperties
                        .getWorkDir()
                        .toString(),
                    MediaFilePolicy
                        .extractExtension(session.uploadName())
                        .orElseThrow()));
                targetPath.set(target);
                return mergeFragmentFiles(sessionDir, target, session.uploadLength());
            }))
            .flatMap(filePath -> repository
                .existsByTypeAndParentIdAndName(
                    AttachmentType.File, parentId, session.uploadName())
                .filter(Boolean::booleanValue)
                .map(exists -> System.currentTimeMillis() + "-" + session.uploadName())
                .switchIfEmpty(Mono.just(session.uploadName()))
                .flatMap(name -> findPathByParentId(parentId, name)
                    .map(path -> AttachmentEntity
                        .builder()
                        .parentId(parentId)
                        .fsPath(filePath.toString())
                        .updateTime(LocalDateTime.now())
                        .type(AttachmentType.File)
                        .name(name)
                        .path(path)
                        .url(path2url(filePath.toString(),
                            ikarosProperties
                                .getWorkDir()
                                .toString()))
                        .size(findFileSize(filePath.toString()))
                        .sha1(findFileSha1(filePath.toString()))
                        .build())
                    .flatMap(this::saveEntity)))
            .then();
    }

    private Mono<Path> mergeFragmentFiles(Path sessionDir, Path targetPath,
                                          long uploadLength) {
        return Mono
            .fromCallable(() -> {
                Files.createDirectories(targetPath.getParent());
                List<Path> chunks;
                try (var paths = Files.list(sessionDir)) {
                    chunks = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path
                            .getFileName()
                            .toString()
                            .matches("\\d+"))
                        .sorted(Comparator.comparingLong(path ->
                            Long.parseLong(path
                                .getFileName()
                                .toString())))
                        .toList();
                }
                long mergedLength = 0;
                byte[] buffer = new byte[BUFFER_SIZE];
                try (OutputStream outputStream = Files.newOutputStream(targetPath,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                    for (Path chunk : chunks) {
                        try (InputStream inputStream = Files.newInputStream(chunk)) {
                            int read;
                            while ((read = inputStream.read(buffer)) >= 0) {
                                if (read > 0) {
                                    outputStream.write(buffer, 0, read);
                                    mergedLength += read;
                                }
                            }
                        }
                    }
                }
                if (mergedLength != uploadLength) {
                    throw new AttachmentUploadException("合并文件长度与声明长度不一致", null);
                }
                return targetPath;
            })
            .subscribeOn(Schedulers.boundedElastic());
    }

    private Path fragmentSessionDir(String unique) {
        if (!unique.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("无效的分片上传会话标识");
        }
        Path cacheRoot = Path
            .of(SystemVarUtils.getOsCacheDirPath(ikarosProperties.getWorkDir()))
            .toAbsolutePath()
            .normalize();
        Path sessionDir = cacheRoot
            .resolve(unique)
            .normalize();
        if (!sessionDir.startsWith(cacheRoot)) {
            throw new IllegalArgumentException("分片上传会话路径越界");
        }
        return sessionDir;
    }

    private Mono<Void> cleanupFragmentUpload(Path sessionDir, @Nullable Path targetPath) {
        return Mono
            .fromRunnable(() -> {
                deleteFileQuietly(targetPath);
                if (!Files.exists(sessionDir)) {
                    return;
                }
                try (var paths = Files.walk(sessionDir)) {
                    paths
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException exception) {
                                log.warn("清理分片上传临时资源失败: {}", path.getFileName(),
                                    exception);
                            }
                        });
                } catch (IOException exception) {
                    log.warn("遍历分片上传临时目录失败: {}", sessionDir.getFileName(), exception);
                }
            })
            .subscribeOn(Schedulers.boundedElastic())
            .then();
    }

    /**
     * 缓存目录内持久化的最小分片验证会话信息.
     */
    private record FragmentSession(
        // 已验证的上传文件名。
        String uploadName,
        // 客户端声明的完整文件长度。
        long uploadLength,
        // 首分片检测出的真实格式。
        MediaFileFormat format) {
    }

    @Override
    @MonoCacheEvict
    public Mono<Void> revertFragmentUploadFile(String unique) {
        Assert.hasText(unique, "'unique' must has text.");
        log.debug("exec revertUploadChunkFileAndDir method for unique={}", unique);
        return cleanupFragmentUpload(fragmentSessionDir(unique), null);
    }

    @Override
    @MonoCacheEvict
    public Mono<Attachment> createDirectory(@Nullable UUID parentId, @NotBlank String name) {
        Assert.hasText(name, "'name' must has text.");
        final UUID fParentId =
            Optional
                .ofNullable(parentId)
                .orElse(AttachmentConst.ROOT_DIRECTORY_ID);
        return repository
            .existsById(fParentId)
            .filter(exists -> exists)
            .switchIfEmpty(Mono.error(new AttachmentParentNotFoundException(
                "Parent attachment not found for id = " + fParentId)))
            .flatMap(exists -> findPathByParentId(fParentId, name))
            .map(path -> AttachmentEntity
                .builder()
                .id(UuidV7Utils.generateUuid())
                .parentId(fParentId)
                .name(name)
                .path(path)
                .updateTime(LocalDateTime.now())
                .type(Directory)
                .build())
            .flatMap(repository::insert)
            .flatMap(attachmentEntity -> copyProperties(attachmentEntity, new Attachment()));
    }

    @Override
    @MonoCacheable(value = "attachments:", key = "#id")
    public Mono<List<Attachment>> findAttachmentPathDirsById(UUID id) {
        return findPathDirs(id, new ArrayList<>())
            .flatMap(attEntities -> repository
                .findById(id)
                .filter(attachmentEntity ->
                    (Directory.equals(attachmentEntity.getType()))
                        || (Driver_Directory.equals(attachmentEntity.getType())))
                .flatMap(entity -> {
                    attEntities.add(attEntities.size(), entity);
                    return Mono.just(attEntities);
                })
                .switchIfEmpty(Mono.just(attEntities)))
            .flatMapMany(attEntities -> Flux.fromStream(attEntities.stream()))
            .flatMap(attachmentEntity -> copyProperties(attachmentEntity, new Attachment()))
            .collectList();
    }

    @Override
    @MonoCacheable(value = "attachment:exists:",
        key = "(#parentId?.toString() ?: '') + ' ' + #name")
    public Mono<Boolean> existsByParentIdAndName(@Nullable UUID parentId, String name) {
        Assert.hasText(name, "'name' must has text.");
        if (Objects.isNull(parentId)) {
            parentId = AttachmentConst.ROOT_DIRECTORY_ID;
        }
        return repository.existsByParentIdAndName(parentId, name);
    }

    @Override
    @MonoCacheable(value = "attachment:exists:",
        key = "#type.toString() + ' ' + (#parentId?.toString() ?: '') + ' ' + #name")
    public Mono<Boolean> existsByTypeAndParentIdAndName(AttachmentType type,
                                                        @Nullable UUID parentId,
                                                        String name) {
        Assert.notNull(type, "'type' must not null.");
        Assert.hasText(name, "'name' must has text.");
        if (Objects.isNull(parentId)) {
            parentId = AttachmentConst.ROOT_DIRECTORY_ID;
        }
        return repository.existsByTypeAndParentIdAndName(type, parentId, name);
    }

    private AttachmentDriverFetcher getAttDriverFetcher(
        AttachmentDriverType type, String driverName
    ) {
        Assert.notNull(type, "'type' must not be null.");
        Assert.hasText(driverName, "'driverName' must has text.");
        return extensionComponentsFinder
            .getExtensions(AttachmentDriverFetcher.class)
            .stream()
            .filter(fetcher -> type.equals(fetcher.getDriverType()))
            .filter(fetcher -> driverName.equals(fetcher.getDriverName()))
            .findFirst()
            .orElseThrow(() -> new NoAvailableAttDriverFetcherException(
                "No found available attachment driver fetcher for type: "
                    + type.name() + " driverName: " + driverName
            ));
    }

    @Override
    public Mono<String> getDownloadUrl(UUID aid) {
        return repository
            .findById(aid)
            .filter(att -> att
                .getType()
                .toString()
                .toUpperCase(Locale.ROOT)
                .startsWith("DRIVER_"))
            .map(AttachmentEntity::getDriverId)
            .flatMap(driverRepository::findById)
            .flatMap(driverEntity -> copyProperties(driverEntity, new AttachmentDriver()))
            .flatMap(driver -> {
                AttachmentDriverFetcher driverFetcher =
                    getAttDriverFetcher(driver.getType(), driver.getName());
                return repository
                    .findById(aid)
                    .flatMap(entity -> copyProperties(entity, new Attachment()))
                    .flatMap(driverFetcher::parseDownloadUrl);
            })
            .switchIfEmpty(repository
                .findById(aid)
                .map(AttachmentEntity::getUrl));
    }

    @Override
    public Mono<String> getReadUrl(UUID aid) {
        return repository
            .findById(aid)
            .filter(att -> att
                .getType()
                .toString()
                .toUpperCase(Locale.ROOT)
                .startsWith("DRIVER_"))
            .map(AttachmentEntity::getDriverId)
            .flatMap(driverRepository::findById)
            .flatMap(driverEntity -> copyProperties(driverEntity, new AttachmentDriver()))
            .flatMap(driver -> {
                AttachmentDriverFetcher driverFetcher =
                    getAttDriverFetcher(driver.getType(), driver.getName());
                return repository
                    .findById(aid)
                    .flatMap(entity -> copyProperties(entity, new Attachment()))
                    .flatMap(driverFetcher::parseReadUrl);
            })
            .switchIfEmpty(repository
                .findById(aid)
                .map(att -> {
                    final String url = att.getUrl();
                    return url.startsWith("http")
                        ? url
                        : OpenApiConst.ATT_STREAM_ENDPOINT_PREFIX + '/' + att.getId();
                }));
    }

    @Override
    public Mono<AttachmentStreamVo> getStreamById(UUID aid) {
        return repository
            .findById(aid)
            .flatMap(attachment -> getValidatedStream(attachment, null, null));
    }

    @Override
    public Mono<AttachmentStreamVo> getStreamByIdWithRange(UUID aid, long start, long end) {
        return repository
            .findById(aid)
            .flatMap(attachment -> {
                if (start < 0 || start > end || attachment.getSize() == null
                    || end >= attachment.getSize()) {
                    return Mono.error(new IllegalArgumentException("无效的附件读取范围"));
                }
                return getValidatedStream(attachment, start, end);
            });
    }

    /**
     * 校验文件写入目标，系统总根目录和文件源目录只允许包含文件夹.
     *
     * @param parentId 文件目标父目录标识
     * @return 校验完成信号
     */
    private Mono<Void> validateFileTargetDirectory(UUID parentId) {
        if (AttachmentConst.ROOT_DIRECTORY_ID.equals(parentId)) {
            return Mono.error(new IllegalArgumentException("根目录只能包含文件夹"));
        }
        return repository
            .findById(parentId)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("目标目录不存在")))
            .flatMap(parent -> AttachmentType.Driver_Directory.equals(parent.getType())
                ? Mono.error(new IllegalArgumentException("文件源目录不允许上传或移动文件"))
                : Mono.empty());
    }

    private Mono<AttachmentStreamVo> getValidatedStream(AttachmentEntity attachment,
                                                        Long start, Long end) {
        validateResponseFilename(attachment.getName());
        if (attachment
            .getType()
            .toString()
            .toUpperCase(Locale.ROOT)
            .startsWith("DRIVER_")) {
            return getValidatedDriverStream(attachment, start, end);
        }
        return getValidatedLocalStream(attachment, start, end);
    }

    private Mono<AttachmentStreamVo> getValidatedDriverStream(AttachmentEntity entity,
                                                              Long start, Long end) {
        return driverRepository
            .findById(entity.getDriverId())
            .flatMap(driverEntity -> copyProperties(driverEntity, new AttachmentDriver()))
            .flatMap(driver -> copyProperties(entity, new Attachment())
                .flatMap(attachment -> {
                    AttachmentDriverFetcher fetcher =
                        getAttDriverFetcher(driver.getType(), driver.getName());
                    Supplier<Flux<DataBuffer>> responseSource = start == null
                        ? () -> fetcher.getSteam(attachment)
                        : () -> fetcher.getSteam(attachment, start, end);
                    long contentLength = start == null
                        ? attachment.getSize() : end - start + 1;
                    return validateAndOpenStream(attachment.getName(), contentLength,
                        () -> fetcher.getSteam(attachment), responseSource);
                }));
    }

    private Mono<AttachmentStreamVo> getValidatedLocalStream(AttachmentEntity attachment,
                                                             Long start, Long end) {
        String rawFsPath = attachment.getFsPath();
        if (StringUtils.hasText(rawFsPath) && !rawFsPath.startsWith("http")) {
            validateFsPath(rawFsPath);
        }
        Path path = Path.of(new File(rawFsPath).toURI());
        return Mono
            .fromCallable(() -> Files.size(path))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(size -> {
                Supplier<Flux<DataBuffer>> fullSource = () -> readFile(path);
                Supplier<Flux<DataBuffer>> responseSource = start == null
                    ? fullSource : () -> readFileRange(path, start, end);
                long contentLength = start == null ? size : end - start + 1;
                return validateAndOpenStream(attachment.getName(), contentLength,
                    fullSource, responseSource);
            });
    }

    private Mono<AttachmentStreamVo> validateAndOpenStream(
        String filename, long contentLength, Supplier<Flux<DataBuffer>> validationSource,
        Supplier<Flux<DataBuffer>> responseSource) {
        return mediaValidationService
            .validate(validationSource.get(), filename)
            .flatMap(validated -> validated
                .content()
                .take(1)
                .doOnNext(DataBufferUtils::release)
                .then(Mono.fromSupplier(() -> {
                    AttachmentStreamVo streamVo = new AttachmentStreamVo();
                    streamVo.setContextLength(contentLength);
                    streamVo.setContextType(validated
                        .detectionResult()
                        .mimeType());
                    streamVo.setDataBufferFlux(responseSource
                        .get()
                        .doOnDiscard(DataBuffer.class, DataBufferUtils::release));
                    return streamVo;
                })))
            .doOnDiscard(DataBuffer.class, DataBufferUtils::release);
    }

    private void validateResponseFilename(String filename) {
        try {
            mediaValidationService.validateFilename(filename);
        } catch (IllegalArgumentException exception) {
            throw new AttachmentUploadException("附件媒体格式不受支持", exception);
        }
    }

    private Flux<DataBuffer> readFile(Path path) {
        return DataBufferUtils.readAsynchronousFileChannel(
            () -> AsynchronousFileChannel.open(path, StandardOpenOption.READ),
            new DefaultDataBufferFactory(), BUFFER_SIZE);
    }

    private Flux<DataBuffer> readFileRange(Path path, long start, long end) {
        return Flux.create(sink -> {
            try {
                AsynchronousFileChannel channel = AsynchronousFileChannel.open(
                    path, StandardOpenOption.READ);
                ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
                readChunk(channel, buffer, start, end, sink, () -> {
                    try {
                        channel.close();
                    } catch (IOException exception) {
                        sink.error(exception);
                    }
                });
            } catch (IOException exception) {
                sink.error(exception);
            }
        });
    }


    private void readChunk(AsynchronousFileChannel channel,
                           ByteBuffer buffer,
                           long position,
                           long end,
                           FluxSink<DataBuffer> sink,
                           Runnable onComplete) {

        if (position > end) {
            sink.complete();
            onComplete.run();
            return;
        }

        long bytesToRead = Math.min(buffer.capacity(), end - position + 1);
        buffer.limit((int) bytesToRead);

        channel.read(buffer, position, buffer, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                if (result == -1) {
                    sink.complete();
                    onComplete.run();
                    return;
                }

                attachment.flip();
                byte[] data = new byte[attachment.remaining()];
                attachment.get(data);

                DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(data);
                sink.next(dataBuffer);

                // 准备读取下一块
                attachment.clear();
                readChunk(channel, attachment, position + result, end, sink, onComplete);
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                sink.error(exc);
                onComplete.run();
            }
        });
    }

    @Override
    public Mono<Flux<DataBuffer>> getStreamByIdWithoutRange(UUID aid) {
        return getStreamById(aid).map(AttachmentStreamVo::getDataBufferFlux);
    }

    @Override
    public Mono<String> getUrlWithConditions(UUID attachmentId,
                                             Map<String, Object> conditions) {
        Map<String, Object> finalConditions = conditions == null
            ? Collections.emptyMap() : conditions;
        return repository
            .findById(attachmentId)
            .flatMap(att -> {
                UUID driverId = att.getDriverId();
                if (driverId == null) {
                    return Mono.just(att.getUrl());
                }
                return driverRepository
                    .findById(driverId)
                    .flatMap(driverEntity -> copyProperties(driverEntity,
                        new AttachmentDriver()))
                    .flatMap(driver -> {
                        // 先查附件DTO
                        return copyProperties(att, new Attachment())
                            .flatMap(attachment -> {
                                // 查找匹配的 AttachmentAccessUrlProvider
                                for (AttachmentAccessUrlProvider provider :
                                    extensionComponentsFinder.getExtensions(
                                        AttachmentAccessUrlProvider.class)) {
                                    if (provider.supports(attachment)) {
                                        return provider.getAccessUrl(
                                            attachment, finalConditions);
                                    }
                                }
                                // 回退到driver的parseReadUrl
                                AttachmentDriverFetcher fetcher = getAttDriverFetcher(
                                    driver.getType(), driver.getName());
                                return fetcher.parseReadUrl(attachment);
                            });
                    })
                    .switchIfEmpty(Mono.just(att.getUrl()));
            });
    }

    @Override
    public Mono<List<AccessUrlCondition>> getUrlConditions(UUID attachmentId) {
        return repository
            .findById(attachmentId)
            .flatMap(att -> {
                UUID driverId = att.getDriverId();
                if (driverId == null) {
                    return Mono.just(List.of());
                }
                return driverRepository
                    .findById(driverId)
                    .flatMap(driverEntity -> copyProperties(driverEntity,
                        new AttachmentDriver()))
                    .flatMap(driver ->
                        copyProperties(att, new Attachment())
                            .flatMap(attachment -> {
                                for (AttachmentAccessUrlProvider provider :
                                    extensionComponentsFinder.getExtensions(
                                        AttachmentAccessUrlProvider.class)) {
                                    if (provider.supports(attachment)) {
                                        return Mono.just(
                                            provider.getConditionDefinitions());
                                    }
                                }
                                return Mono.just(List.<AccessUrlCondition>of());
                            }));
            });
    }

    private Mono<List<AttachmentEntity>> findPathDirs(UUID id, List<AttachmentEntity> entities) {
        if (ROOT_DIRECTORY_ID.equals(id)) {
            Collections.reverse(entities);
            return Mono.just(entities);
        }
        return repository
            .findById(id)
            .flatMap(e -> repository
                .findById(e.getParentId())
                .switchIfEmpty(
                    Mono.error(new NotFoundException("att parent not found for " + e))))
            .flatMap(attachmentEntity -> {
                entities.add(attachmentEntity);
                return findPathDirs(attachmentEntity.getId(), entities);
            });
    }

    private Mono<String> findPathByParentId(UUID parentId, String name) {
        if (ROOT_DIRECTORY_ID.equals(parentId)) {
            return Mono.just('/' + name);
        }
        return repository
            .findById(parentId)
            .map(AttachmentEntity::getPath)
            .map(path -> path + '/' + name);
    }

    private AttachmentEntity removeFileSystemFile(AttachmentEntity attachmentEntity) {
        if (Directory.equals(attachmentEntity.getType())
            || attachmentEntity
            .getType()
            .toString()
            .toUpperCase(Locale.ROOT)
            .startsWith("DRIVER")) {
            return attachmentEntity;
        }
        String fsPath = attachmentEntity.getFsPath();
        if (!StringUtils.hasText(fsPath) || fsPath.startsWith("http")) {
            return attachmentEntity;
        }
        // Path traversal prevention: validate fsPath before deleting
        validateFsPath(fsPath);
        try {
            Files.deleteIfExists(Path.of(fsPath));
        } catch (IOException e) {
            throw new AttachmentRemoveException(
                "Attachment delete fail for file system path：" + fsPath, e);
        }
        return attachmentEntity;
    }

    /**
     * Validate that the fsPath is contained within the application work directory
     * to prevent path traversal attacks (CWE-22).
     *
     * @param fsPath the file system path to validate
     * @throws IllegalArgumentException if fsPath escapes the work directory
     */
    private void validateFsPath(String fsPath) {
        Path path = Path.of(fsPath);
        if (!path.isAbsolute()) {
            // 相对路径：检查是否存在路径穿越（../）
            // 如 ../../etc/passwd 会包含 ..，而网盘驱动标识符如 "0" 则不会
            Path normalized = path.normalize();
            if (normalized
                .toString()
                .contains("..")) {
                throw new IllegalArgumentException(
                    "Path traversal detected in fsPath: " + fsPath);
            }
            return;
        }
        Path normalized = path.normalize();
        Path workDirPath = ikarosProperties
            .getWorkDir()
            .normalize();
        if (!normalized.startsWith(workDirPath)) {
            throw new IllegalArgumentException(
                "fsPath escapes work directory: " + fsPath);
        }
    }
}
