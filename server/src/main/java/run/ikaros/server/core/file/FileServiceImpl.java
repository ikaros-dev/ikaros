package run.ikaros.server.core.file;

import static run.ikaros.server.infra.utils.ReactiveBeanUtils.copyProperties;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.ikaros.api.constant.FileConst;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.infra.exception.file.FileExistsException;
import run.ikaros.api.infra.exception.file.FolderNotFoundException;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.api.infra.utils.SystemVarUtils;
import run.ikaros.api.store.enums.FileType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.file.event.FileRemoveEvent;
import run.ikaros.server.core.file.event.FileSaveEvent;
import run.ikaros.server.core.file.task.FileDeleteRemoteTask;
import run.ikaros.server.core.file.task.FilePull4RemoteTask;
import run.ikaros.server.core.file.task.FilePush2RemoteTask;
import run.ikaros.server.core.task.TaskService;
import run.ikaros.server.plugin.ExtensionComponentsFinder;
import run.ikaros.server.store.entity.FileEntity;
import run.ikaros.server.store.entity.FolderEntity;
import run.ikaros.server.store.entity.TaskEntity;
import run.ikaros.server.store.repository.EpisodeFileRepository;
import run.ikaros.server.store.repository.FileRemoteRepository;
import run.ikaros.server.store.repository.FileRepository;
import run.ikaros.server.store.repository.FolderRepository;
import run.ikaros.server.store.repository.TaskRepository;

@Slf4j
@Service
public class FileServiceImpl implements FileService, ApplicationContextAware {
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final EpisodeFileRepository episodeFileRepository;
    private final FileRemoteRepository fileRemoteRepository;
    private final IkarosProperties ikarosProperties;
    private final R2dbcEntityTemplate template;
    private ApplicationContext applicationContext;
    private final ExtensionComponentsFinder extensionComponentsFinder;
    private final ReactiveCustomClient reactiveCustomClient;
    private final TaskService taskService;
    private final TaskRepository taskRepository;

    /**
     * Construct.
     */
    public FileServiceImpl(FileRepository fileRepository,
                           FolderRepository folderRepository,
                           EpisodeFileRepository episodeFileRepository,
                           FileRemoteRepository fileRemoteRepository,
                           IkarosProperties ikarosProperties,
                           R2dbcEntityTemplate template,
                           ExtensionComponentsFinder extensionComponentsFinder,
                           ReactiveCustomClient reactiveCustomClient, TaskService taskService,
                           TaskRepository taskRepository) {
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.episodeFileRepository = episodeFileRepository;
        this.fileRemoteRepository = fileRemoteRepository;
        this.ikarosProperties = ikarosProperties;
        this.template = template;
        this.extensionComponentsFinder = extensionComponentsFinder;
        this.reactiveCustomClient = reactiveCustomClient;
        this.taskService = taskService;
        this.taskRepository = taskRepository;
    }

    @Override
    public Mono<Void> receiveAndHandleFragmentUploadChunkFile(@NotBlank String unique,
                                                              @NotNull Long uploadLength,
                                                              @NotNull Long uploadOffset,
                                                              @NotBlank String uploadName,
                                                              byte[] bytes) {
        Assert.hasText(unique, "'unique' must has text.");
        Assert.notNull(uploadLength, "'uploadLength' must not null.");
        Assert.notNull(uploadOffset, "'uploadOffset' must not null.");
        Assert.hasText(uploadName, "'uploadName' must has text.");
        Assert.notNull(bytes, "'bytes' must not null.");
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

            File file = new File(filePath);
            Flux<DataBuffer> dataBufferFlux = FileUtils.convertToDataBufferFlux(file);

            return upload(uploadName, dataBufferFlux).then();
        }
        return Mono.empty();
    }

    @Override
    public Mono<Void> revertFragmentUploadFile(@NotBlank String unique) {
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
    public Mono<FileEntity> updateEntity(FileEntity fileEntity) {
        Assert.notNull(fileEntity, "'fileEntity' must not null.");
        return save(fileEntity.setUpdateTime(LocalDateTime.now()));
    }

    @Override
    public Mono<PagingWrap<FileEntity>> listEntitiesByCondition(
        @NotNull FindFileCondition condition) {
        Assert.notNull(condition, "'condition' must no null.");

        final Integer page = condition.getPage();
        Assert.isTrue(page > 0, "'page' must gt 0.");

        final Integer size = condition.getSize();
        Assert.isTrue(size > 0, "'size' must gt 0.");

        final String fileName = StringUtils.hasText(condition.getFileName())
            ? condition.getFileName() : "";
        final String fileNameLike = "%" + fileName + "%";
        final FileType type = condition.getType();

        PageRequest pageRequest = PageRequest.of(page - 1, size);

        Flux<FileEntity> fileEntityFlux;
        Mono<Long> countMono;

        // todo 使用 R2dbcEntityTemplate 进行动态拼接
        if (type == null) {
            fileEntityFlux = fileRepository.findAllByNameLike(fileNameLike, pageRequest);
            countMono = fileRepository.countAllByNameLike(fileNameLike);
        } else {
            fileEntityFlux =
                fileRepository.findAllByNameLikeAndType(fileNameLike, type, pageRequest);
            countMono = fileRepository.countAllByNameLikeAndType(fileNameLike, type);
        }

        Mono<Long> finalCountMono = countMono;
        return fileEntityFlux
            .collectList()
            .flatMap(fileEntities -> finalCountMono
                .map(count -> new PagingWrap<>(page,
                    size, count, fileEntities)));
    }

    @Override
    public Flux<FileEntity> findAll() {
        return fileRepository.findAll();
    }

    @Override
    public Mono<FileEntity> findById(Long id) {
        Assert.isTrue(id > 0, "'id' must gt 0.");
        return fileRepository.findById(id);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        Assert.isTrue(id > 0, "'id' must gt 0.");
        return Mono.just(id)
            .flatMap(this::findById)
            .map(entity -> {
                File file = new File(entity.getFsPath());
                if (file.exists()) {
                    file.delete();
                    log.debug("delete local file in path: {}", file.getAbsolutePath());
                }
                return entity;
            })
            .flatMap(fileEntity -> fileRepository.deleteById(id)
                .doOnSuccess(unused -> applicationContext.publishEvent(
                    new FileRemoveEvent(this, fileEntity))))
            .checkpoint("DeleteFileEntityByFileId")
            .then(episodeFileRepository.deleteAllByFileId(id))
            .doOnNext(lines -> {
                if (lines > 0) {
                    log.debug("Delete all episode file records, counts: [{}], "
                        + "when delete file entity for id [{}].", lines, id);
                }
            })
            .checkpoint("DeleteAllEpisodeFileByFileIdAfterDeleteFileEntity")
            .then(fileRemoteRepository.existsAllByFileId(id))
            .filter(exists -> exists)
            // 如果存在，则提交删除远端文件任务
            .flatMap(exists -> Mono.defer((Supplier<Mono<Void>>) () -> {
                FileDeleteRemoteTask fileDeleteRemoteTask =
                    new FileDeleteRemoteTask(TaskEntity.builder().build(),
                        taskRepository, applicationContext, id);
                return taskService.submit(fileDeleteRemoteTask);
            }))
            .then();
    }

    @Override
    public Mono<FileEntity> save(FileEntity entity) {
        Assert.notNull(entity, "'entity' must not null.");
        return fileRepository.save(entity)
            .doOnSuccess(fileEntity ->
                applicationContext.publishEvent(new FileSaveEvent(this, fileEntity)));
    }

    @Override
    public Mono<run.ikaros.api.core.file.File> upload(String fileName,
                                                      Flux<DataBuffer> dataBufferFlux) {
        return upload(fileName, true, dataBufferFlux);
    }

    @Override
    public Mono<run.ikaros.api.core.file.File> upload(String fileName, Boolean isAutoReName,
                                                      Flux<DataBuffer> dataBufferFlux) {
        LocalDateTime now = LocalDateTime.now();
        String year = String.valueOf(now.getYear());
        String month = String.valueOf(now.getMonth().getValue());
        String day = String.valueOf(now.getDayOfMonth());


        // create default upload folder if not exists
        return folderRepository.findByNameAndParentId(FileConst.DEFAULT_UPLOAD_FOLDER_NAME,
                FileConst.DEFAULT_FOLDER_ROOT_ID)
            .switchIfEmpty(folderRepository.save(FolderEntity.builder()
                .parentId(FileConst.DEFAULT_FOLDER_ROOT_ID)
                .name(FileConst.DEFAULT_UPLOAD_FOLDER_NAME)
                .updateTime(now)
                .build()))
            .map(FolderEntity::getId)

            // create sub folder by year
            .flatMap(parentFolderId -> folderRepository.findByNameAndParentId(
                    year, parentFolderId)
                .switchIfEmpty(folderRepository.save(FolderEntity.builder()
                    .parentId(parentFolderId)
                    .name(year)
                    .updateTime(now)
                    .build()))
                .map(FolderEntity::getId)
            )

            // create sub folder by month
            .flatMap(parentFolderId -> folderRepository.findByNameAndParentId(
                    month, parentFolderId)
                .switchIfEmpty(folderRepository.save(FolderEntity.builder()
                    .parentId(parentFolderId)
                    .name(month)
                    .updateTime(now)
                    .build()))
                .map(FolderEntity::getId)
            )

            // create sub folder by day
            .flatMap(parentFolderId -> folderRepository.findByNameAndParentId(
                    day, parentFolderId)
                .switchIfEmpty(folderRepository.save(FolderEntity.builder()
                    .parentId(parentFolderId)
                    .name(day)
                    .updateTime(now)
                    .build()))
                .map(FolderEntity::getId)
            )

            // upload file to folder
            .flatMap(folderId -> upload(folderId, fileName, isAutoReName, dataBufferFlux))
            ;

    }

    @Override
    public Mono<run.ikaros.api.core.file.File> upload(Long folderId, String fileName,
                                                      Boolean isAutoReName,
                                                      Flux<DataBuffer> dataBufferFlux)
        throws FolderNotFoundException, FileExistsException {
        Assert.isTrue(folderId >= 0, "'folderId' must >= 0");
        Assert.hasText(fileName, "'fileName' must has text");
        Assert.notNull(dataBufferFlux, "'dataBufferFlux' must not null");
        if (Objects.isNull(isAutoReName)) {
            isAutoReName = true;
        }

        // build file upload path
        String uploadFilePath =
            FileUtils.buildAppUploadFilePath(ikarosProperties.getWorkDir().toString(),
                FileUtils.parseFilePostfix(fileName));

        // check folder is exists
        Boolean finalIsAutoReName = isAutoReName;
        return folderRepository.existsById(folderId)
            .filter(exists -> exists)
            .switchIfEmpty(
                Mono.error(new FolderNotFoundException("folder not found for id: " + folderId)))
            // check file folder_id and name has exists
            .then(fileRepository.existsByFolderIdAndName(folderId, fileName))
            .filter(exists -> !exists)
            .map(exists -> fileName)
            .switchIfEmpty(Mono.just(fileName)
                .<String>handle((name, sink) -> {
                    if (finalIsAutoReName) {
                        name = FileUtils.parseFileNameWithoutPostfix(name)
                            + " - " + UUID.randomUUID().toString().replace("-", "")
                            + "."
                            + FileUtils.parseFilePostfix(name);
                        sink.next(name);
                    } else {
                        sink.error(new FileExistsException(
                            "current file has exists for folder id: " + folderId
                                + " fileName: " + fileName));
                    }
                }))

            // build file entity
            .map(newFileName -> FileEntity.builder()
                .folderId(FileConst.DEFAULT_FOLDER_ID)
                .type(FileUtils.parseTypeByPostfix(FileUtils.parseFilePostfix(fileName)))
                .url(path2url(uploadFilePath, ikarosProperties.getWorkDir().toString()))
                .name(newFileName)
                .fsPath(uploadFilePath)
                .folderId(folderId)
                .canRead(true)
                .updateTime(LocalDateTime.now())
                .build())

            // upload file to server path
            .flatMap(fileEntity -> writeToImportPath(dataBufferFlux, Path.of(uploadFilePath))
                .publishOn(Schedulers.boundedElastic())
                .map(path -> fileEntity))

            // save file entity to database
            .flatMap(this::save)
            .flatMap(fileEntity -> copyProperties(fileEntity,
                new run.ikaros.api.core.file.File()))
            ;
    }

    private Mono<Void> pushRemote(FileEntity fileEntity, String remote) {
        Assert.notNull(fileEntity, "'fileEntity' must not null.");
        Assert.hasText(remote, "'remote' must has text.");

        if (!fileEntity.getCanRead() && !StringUtils.hasText(fileEntity.getUrl())) {
            log.warn("skip push operate, current file entity exists in remote for file: {}",
                fileEntity);
            return Mono.empty();
        }

        // 远端表存在对应的记录，代表已经 push 过，则只需要更新文件记录并删除文件即可
        return fileRemoteRepository.findAllByFileIdAndRemote(fileEntity.getId(), remote)
            .collectList()
            .filter(list -> list != null && !list.isEmpty())
            .flatMap(list -> fileRepository.findById(fileEntity.getId())
                .<FileEntity>handle((fileEntity1, sink) -> {
                    log.debug("current file remote records has exists, skip push operate.");
                    String originalPath = fileEntity1.getFsPath();
                    try {
                        Files.delete(Path.of(originalPath));
                        log.debug("delete file in path: {}", originalPath);
                    } catch (IOException e) {
                        sink.error(new RuntimeException(e));
                        return;
                    }
                    sink.next(fileEntity1);
                })
                .map(fileEntity1 -> fileEntity1.setCanRead(false)
                    .setUrl("").setFsPath(""))
                .flatMap(fileRepository::save).then()
            )
            .then(fileRemoteRepository.findAllByFileIdAndRemote(fileEntity.getId(), remote)
                .collectList())
            .filter(list -> list == null || list.isEmpty())
            .flatMap(list -> Mono.defer((Supplier<Mono<Void>>) () -> {
                FilePush2RemoteTask filePush2RemoteTask =
                    new FilePush2RemoteTask(TaskEntity.builder().build(), taskRepository,
                        applicationContext, fileEntity.getId(), remote);
                return taskService.submit(filePush2RemoteTask);
            }).then());
    }


    @Override
    public Mono<Void> pushRemote(Long fileId, String remote) {
        Assert.isTrue(fileId > 0, "'fileId' must gt 0.");
        Assert.hasText(remote, "'remote' must has text.");
        return fileRepository.findById(fileId)
            .switchIfEmpty(
                Mono.error(new NotFoundException("not found file entity for id is " + fileId)))
            .flatMap(fileEntity -> pushRemote(fileEntity, remote));
    }

    @Override
    public Mono<Void> pullRemote(Long fileId, String remote) {
        Assert.isTrue(fileId > 0, "'fileId' must gt 0.");
        Assert.hasText(remote, "'remote' must has text.");
        return fileRepository.findById(fileId)
            .filter(fileEntity -> !fileEntity.getCanRead()
                && !StringUtils.hasText(fileEntity.getUrl()))
            .switchIfEmpty(Mono.error(new RuntimeException("please push remote before pull.")))
            .flatMap(fileEntity -> Mono.defer((Supplier<Mono<Void>>) () -> {
                FilePull4RemoteTask filePull4RemoteTask =
                    new FilePull4RemoteTask(TaskEntity.builder().build(), taskRepository,
                        applicationContext, fileEntity.getId(), remote);
                return taskService.submit(filePull4RemoteTask);
            }));
    }

    @Override
    public Mono<Void> pushRemoteBatch(List<Long> fileIds, String remote) {
        Assert.notNull(fileIds, "'fileIds' must not null.");
        Assert.hasText(remote, "'remote' must has text.");
        return Flux.fromStream(fileIds.stream())
            .flatMap(fileId -> pushRemote(fileId, remote))
            .then();
    }

    @Override
    public Mono<Void> pullRemoteBatch(List<Long> fileIds, String remote) {
        Assert.notNull(fileIds, "'fileIds' must not null.");
        Assert.hasText(remote, "'remote' must has text.");
        return Flux.fromStream(fileIds.stream())
            .flatMap(fileId -> pullRemote(fileId, remote))
            .then();
    }

    @Override
    public Mono<run.ikaros.api.core.file.File> updateFolder(Long id, Long folderId) {
        Assert.isTrue(id > -1, "id must gt -1.");
        Assert.isTrue(folderId > -1, "folderId must gt -1.");
        return findById(id)
            .map(fileEntity -> fileEntity.setFolderId(folderId).setUpdateTime(LocalDateTime.now()))
            .flatMap(fileRepository::save)
            .flatMap(fileEntity -> copyProperties(fileEntity,
                new run.ikaros.api.core.file.File()));
    }

    @Override
    public Mono<Boolean> existsByFolderIdAndFileName(Long folderId, String fileName) {
        Assert.isTrue(folderId >= 0, "'folderId' must >= 0");
        Assert.hasText(fileName, "'fileName' must has text.");
        return fileRepository.existsByFolderIdAndName(folderId, fileName);
    }


    private static Mono<Path> writeToImportPath(Flux<DataBuffer> dataBufferFlux,
                                                Path importPath) {
        File file = importPath.toFile();
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
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
                    e.printStackTrace();
                }
            })
            .then(Mono.just(importPath));
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
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
