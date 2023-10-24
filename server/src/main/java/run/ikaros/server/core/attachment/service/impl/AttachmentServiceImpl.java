package run.ikaros.server.core.attachment.service.impl;

import static run.ikaros.api.core.attachment.AttachmentConst.ROOT_DIRECTORY_ID;
import static run.ikaros.api.core.attachment.AttachmentConst.ROOT_DIRECTORY_PARENT_ID;
import static run.ikaros.api.store.enums.AttachmentType.Directory;
import static run.ikaros.server.infra.utils.ReactiveBeanUtils.copyProperties;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import run.ikaros.api.core.attachment.AttachmentConst;
import run.ikaros.api.core.attachment.AttachmentSearchCondition;
import run.ikaros.api.core.attachment.AttachmentUploadCondition;
import run.ikaros.api.core.attachment.exception.AttachmentParentNotFoundException;
import run.ikaros.api.core.attachment.exception.AttachmentRemoveException;
import run.ikaros.api.core.attachment.exception.AttachmentUploadException;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.api.infra.utils.SystemVarUtils;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;
import run.ikaros.server.store.repository.AttachmentRepository;

@Slf4j
@Service
public class AttachmentServiceImpl implements AttachmentService {
    private final AttachmentRepository repository;
    private final AttachmentReferenceRepository referenceRepository;
    private final R2dbcEntityTemplate template;
    private final IkarosProperties ikarosProperties;

    /**
     * Construct.
     */
    public AttachmentServiceImpl(AttachmentRepository repository,
                                 AttachmentReferenceRepository referenceRepository,
                                 R2dbcEntityTemplate template, IkarosProperties ikarosProperties) {
        this.repository = repository;
        this.referenceRepository = referenceRepository;
        this.template = template;
        this.ikarosProperties = ikarosProperties;
    }

    @Override
    public Mono<AttachmentEntity> saveEntity(AttachmentEntity attachmentEntity) {
        Assert.notNull(attachmentEntity, "'attachmentEntity' must not be null.");
        return findPathByParentId(attachmentEntity.getParentId(), attachmentEntity.getName())
            .map(attachmentEntity::setPath)
            .map(entity -> entity.setUpdateTime(LocalDateTime.now()))
            .flatMap(repository::save);
    }

    @Override
    public Mono<Attachment> save(Attachment attachment) {
        Assert.notNull(attachment, "'attachment' must not be null.");
        attachment.setParentId(Optional.ofNullable(attachment.getParentId())
            .orElse(AttachmentConst.ROOT_DIRECTORY_ID));
        Mono<AttachmentEntity> attachmentEntityMono =
            Objects.isNull(attachment.getId())
                ? copyProperties(attachment, new AttachmentEntity())
                : repository.findById(attachment.getId())
                .flatMap(attachmentEntity -> copyProperties(attachment, attachmentEntity));
        return attachmentEntityMono
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
        final PageRequest pageRequest = PageRequest.of(page - 1, size);

        Criteria criteria = Criteria.empty();

        if (Objects.nonNull(parentId)) {
            criteria = Criteria.where("parent_id").is(parentId);
        }

        if (Objects.nonNull(type)) {
            criteria = criteria.and("type").is(type);
        }

        if (StringUtils.hasText(name)) {
            criteria = criteria.and("name").like(nameLike);
        }

        Query query = Query.query(criteria)
            .sort(Sort.by(Sort.Order.by("type")))
            .with(pageRequest);

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

    private Mono<AttachmentEntity> checkChildAttachmentRefNotExists(
        AttachmentEntity attachmentEntity) {
        return Mono.just(attachmentEntity)
            .map(AttachmentEntity::getType)
            .filter(Directory::equals)
            .map(eq -> attachmentEntity.getId())
            .flatMapMany(repository::findAllByParentId)
            .flatMap(entity -> referenceRepository.existsByAttachmentId(entity.getId())
                .filter(exists -> !exists)
                .switchIfEmpty(Mono.error(new AttachmentRemoveException(
                    "Forbid remove, attachment refs exists, "
                        + "please remove all refs for current attachment before remove it, id="
                        + entity.getId() + " and name=" + entity.getName()))))
            .then(Mono.just(attachmentEntity));
    }

    private Mono<Long> checkAttachmentRefNotExists(Long attachmentId) {
        return repository.findById(attachmentId)
            .flatMap(this::checkChildAttachmentRefNotExists)
            .flatMap(entity -> referenceRepository.existsByAttachmentId(entity.getId())
                .filter(exists -> !exists)
                .switchIfEmpty(Mono.error(new AttachmentRemoveException(
                    "Forbid remove, attachment refs exists, "
                        + "please remove all refs for current attachment before remove it, id="
                        + entity.getId() + " and name=" + entity.getName()))))
            .then(Mono.just(attachmentId));
    }

    private Mono<AttachmentEntity> removeChildrenAttachment(AttachmentEntity attachmentEntity) {
        return Mono.just(attachmentEntity)
            .map(AttachmentEntity::getType)
            .filter(Directory::equals)
            .map(eq -> attachmentEntity.getId())
            .flatMapMany(repository::findAllByParentId)
            .flatMap(this::removeChildrenAttachment)
            .switchIfEmpty(Mono.just(attachmentEntity))
            .map(this::removeFileSystemFile)
            .flatMap(repository::delete)
            .then(Mono.just(attachmentEntity));
    }


    @Override
    public Mono<Void> removeById(Long attachmentId) {
        Assert.isTrue(attachmentId > 0, "'attachmentId' must gt 0.");
        if (AttachmentConst.COVER_DIRECTORY_ID.equals(attachmentId)
            || AttachmentConst.DOWNLOAD_DIRECTORY_ID.equals(attachmentId)) {
            return Mono.error(new AttachmentRemoveException(
                "Forbid remove system internal 'Covers' or 'Downloads' dir for attachment id="
                    + attachmentId));
        }
        return checkAttachmentRefNotExists(attachmentId)
            .flatMap(repository::findById)
            .flatMap(this::removeChildrenAttachment)
            .map(this::removeFileSystemFile)
            .flatMap(repository::delete);
    }


    @Override
    public Mono<Void> removeByTypeAndParentIdAndName(AttachmentType type,
                                                     @Nullable Long parentId, String name) {
        Assert.notNull(type, "'type' must not null.");
        Assert.hasText(name, "'name' must has text.");
        if (Objects.isNull(parentId)) {
            parentId = AttachmentConst.ROOT_DIRECTORY_ID;
        }
        return repository.findByTypeAndParentIdAndName(type, parentId, name)
            .map(AttachmentEntity::getId)
            .flatMap(this::removeById);
    }

    @Override
    public Mono<Attachment> upload(AttachmentUploadCondition uploadCondition) {
        Assert.notNull(uploadCondition, "'uploadCondition' must not null.");
        String name = uploadCondition.getName();
        final Boolean isAutoReName =
            Optional.ofNullable(uploadCondition.getIsAutoReName()).orElse(true);
        Long parentId = Optional.ofNullable(uploadCondition.getParentId())
            .orElse(AttachmentConst.ROOT_DIRECTORY_ID);

        // build file upload path
        String uploadFilePath =
            FileUtils.buildAppUploadFilePath(ikarosProperties.getWorkDir().toString(),
                FileUtils.parseFilePostfix(name));

        // upload file data buffer
        return writeDataToFsPath(uploadCondition.getDataBufferFlux(), Path.of(uploadFilePath))
            //.publishOn(Schedulers.boundedElastic())
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
                        findPathByParentId(parentId, n)
                            .map(path -> AttachmentEntity.builder()
                                .parentId(parentId)
                                .fsPath(fsPath.toString())
                                .updateTime(LocalDateTime.now())
                                .type(AttachmentType.File)
                                .name(n)
                                .path(path)
                                .url(path2url(uploadFilePath,
                                    ikarosProperties.getWorkDir().toString()))
                                .size(findFileSize(uploadFilePath))
                                .build())
                            .flatMap(this::saveEntity)
                    )
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
        if (Objects.isNull(parentId)) {
            parentId = AttachmentConst.ROOT_DIRECTORY_ID;
        }
        return repository.findByTypeAndParentIdAndName(type, parentId, name)
            .flatMap(attachmentEntity -> copyProperties(attachmentEntity, new Attachment()));
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
                                                              String uploadName, byte[] bytes,
                                                              @Nullable Long parentId) {
        Assert.hasText(unique, "'unique' must has text.");
        Assert.notNull(uploadLength, "'uploadLength' must not null.");
        Assert.notNull(uploadOffset, "'uploadOffset' must not null.");
        Assert.hasText(uploadName, "'uploadName' must has text.");
        Assert.notNull(bytes, "'bytes' must not null.");
        if (Objects.isNull(parentId)) {
            parentId = AttachmentConst.ROOT_DIRECTORY_ID;
        }
        Path workDir = ikarosProperties.getWorkDir();
        File tempChunkFileCacheDir =
            new File(SystemVarUtils.getOsCacheDirPath(workDir) + File.separator + unique);
        if (!tempChunkFileCacheDir.exists()) {
            tempChunkFileCacheDir.mkdirs();
            log.debug("create temp dir: {}", tempChunkFileCacheDir);
        }

        Assert.notNull(bytes, "file bytes must not be null");

        long offset = uploadOffset + bytes.length;
        File uploadedChunkCacheFile = new File(tempChunkFileCacheDir + File.separator + offset);
        try {
            Files.write(Path.of(uploadedChunkCacheFile.toURI()), bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.debug("upload chunk[{}] to path: {}", uploadOffset,
            uploadedChunkCacheFile.getAbsolutePath());

        if (offset == uploadLength) {
            String postfix = uploadName.substring(uploadName.lastIndexOf(".") + 1);
            final String filePath;
            try {
                filePath = meringTempChunkFile(unique, postfix);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            for (File file : Objects.requireNonNull(tempChunkFileCacheDir.listFiles())) {
                file.delete();
            }
            tempChunkFileCacheDir.delete();

            Long finalParentId = parentId;
            return
                // rename if  exists same file.
                repository.existsByTypeAndParentIdAndName(
                        AttachmentType.File, parentId, uploadName
                    )
                    .filter(exists -> exists)
                    .map(exists -> System.currentTimeMillis() + "-" + uploadName)
                    .switchIfEmpty(Mono.just(uploadName))
                    .flatMap(n ->
                        // save attachment entity
                        findPathByParentId(finalParentId, n)
                            .map(path -> AttachmentEntity.builder()
                                .parentId(finalParentId)
                                .fsPath(filePath)
                                .updateTime(LocalDateTime.now())
                                .type(AttachmentType.File)
                                .name(n)
                                .path(path)
                                .url(path2url(filePath, ikarosProperties.getWorkDir().toString()))
                                .size(findFileSize(filePath))
                                .build())
                            .flatMap(this::saveEntity)
                    ).then();
        }
        return Mono.empty();
    }

    private String meringTempChunkFile(String unique, String postfix) throws IOException {
        log.debug("All chunks upload has finish, will start merging files");

        Path workDir = ikarosProperties.getWorkDir();
        File targetFile = new File(FileUtils.buildAppUploadFilePath(
            workDir.toFile().getAbsolutePath(), postfix));
        String absolutePath = targetFile.getAbsolutePath();

        String chunkFileDirPath =
            SystemVarUtils.getOsCacheDirPath(workDir) + File.separator + unique;
        File chunkFileDir = new File(chunkFileDirPath);
        File[] files = chunkFileDir.listFiles();
        List<File> chunkFileList = Arrays.asList(files);
        // PS: 这里需要根据文件名(偏移量)升序, 不然合并的文件分片内容的顺序不正常
        Collections.sort(chunkFileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                long o1Offset = Long.parseLong(o1.getName());
                long o2Offset = Long.parseLong(o2.getName());
                if (o1Offset < o2Offset) {
                    return -1;
                } else if (o1Offset > o2Offset) {
                    return 1;
                }
                return 0;
            }

            @Override
            public boolean equals(Object obj) {
                return false;
            }
        });
        int targetFileWriteOffset = 0;
        for (File chunkFile : chunkFileList) {
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(targetFile, "rw");
                 FileInputStream fileInputStream = new FileInputStream(chunkFile);) {
                randomAccessFile.seek(targetFileWriteOffset);
                byte[] bytes = new byte[fileInputStream.available()];
                int read = fileInputStream.read(bytes);
                randomAccessFile.write(bytes);
                targetFileWriteOffset += read;
                log.debug("[{}] current merge targetFileWriteOffset: {}", chunkFile.getName(),
                    targetFileWriteOffset);
            }
        }

        log.info("Merging all chunk files success, absolute path: {}", absolutePath);
        return absolutePath;
    }

    @Override
    public Mono<Void> revertFragmentUploadFile(String unique) {
        Assert.hasText(unique, "'unique' must has text.");
        log.debug("exec revertUploadChunkFileAndDir method for unique={}", unique);
        Path workDir = ikarosProperties.getWorkDir();
        String fileChunkCacheDirPath =
            SystemVarUtils.getOsCacheDirPath(workDir) + File.separator + unique;
        File fileChunkCacheDir = new File(fileChunkCacheDirPath);
        if (fileChunkCacheDir.exists()) {
            for (File file : Objects.requireNonNull(fileChunkCacheDir.listFiles())) {
                if (file.exists()) {
                    file.delete();
                }
            }
            fileChunkCacheDir.delete();
            log.debug("remove uploading file with unique={}", unique);
        }
        return Mono.empty();
    }

    @Override
    public Mono<Attachment> createDirectory(@Nullable Long parentId, @NotBlank String name) {
        Assert.hasText(name, "'name' must has text.");
        final Long fParentId =
            Optional.ofNullable(parentId).orElse(AttachmentConst.ROOT_DIRECTORY_ID);
        return repository.existsById(fParentId)
            .filter(exists -> exists)
            .switchIfEmpty(Mono.error(new AttachmentParentNotFoundException(
                "Parent attachment not found for id = " + fParentId)))
            .flatMap(exists -> findPathByParentId(fParentId, name))
            .map(path -> AttachmentEntity.builder()
                .parentId(fParentId)
                .name(name)
                .path(path)
                .updateTime(LocalDateTime.now())
                .type(Directory)
                .build())
            .flatMap(repository::save)
            .flatMap(attachmentEntity -> copyProperties(attachmentEntity, new Attachment()));
    }

    @Override
    public Mono<List<Attachment>> findAttachmentPathDirsById(Long id) {
        Assert.isTrue(id >= 0, "'id' must >= 0.");
        return findPathDirs(id, new ArrayList<>())
            .flatMap(attEntities -> repository.findById(id)
                .filter(attachmentEntity -> Directory.equals(attachmentEntity.getType()))
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
    public Mono<Boolean> existsByParentIdAndName(@Nullable Long parentId, String name) {
        Assert.hasText(name, "'name' must has text.");
        if (Objects.isNull(parentId)) {
            parentId = AttachmentConst.ROOT_DIRECTORY_ID;
        }
        return repository.existsByParentIdAndName(parentId, name);
    }

    @Override
    public Mono<Boolean> existsByTypeAndParentIdAndName(AttachmentType type,
                                                        @Nullable Long parentId,
                                                        String name) {
        Assert.notNull(type, "'type' must not null.");
        Assert.hasText(name, "'name' must has text.");
        if (Objects.isNull(parentId)) {
            parentId = AttachmentConst.ROOT_DIRECTORY_ID;
        }
        return repository.existsByTypeAndParentIdAndName(type, parentId, name);
    }

    private Mono<List<AttachmentEntity>> findPathDirs(long id, List<AttachmentEntity> entities) {
        if (ROOT_DIRECTORY_PARENT_ID.equals(id)) {
            Collections.reverse(entities);
            return Mono.just(entities);
        }
        return repository.findById(id)
            .map(AttachmentEntity::getParentId)
            .flatMap(repository::findById)
            .flatMap(attachmentEntity -> {
                entities.add(attachmentEntity);
                return findPathDirs(attachmentEntity.getId(), entities);
            });
    }

    private Mono<String> findPathByParentId(Long parentId, String name) {
        if (ROOT_DIRECTORY_ID.equals(parentId)) {
            return Mono.just('/' + name);
        }
        return repository.findById(parentId)
            .map(AttachmentEntity::getPath)
            .map(path -> path + '/' + name);
    }

    private AttachmentEntity removeFileSystemFile(AttachmentEntity attachmentEntity) {
        if (Directory.equals(attachmentEntity.getType())) {
            return attachmentEntity;
        }
        String fsPath = attachmentEntity.getFsPath();
        if (!StringUtils.hasText(fsPath)) {
            return attachmentEntity;
        }
        try {
            Files.deleteIfExists(Path.of(fsPath));
        } catch (IOException e) {
            throw new AttachmentRemoveException(
                "Attachment delete fail for file system path：" + fsPath, e);
        }
        return attachmentEntity;
    }
}
