package run.ikaros.server.core.file;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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
import run.ikaros.api.core.file.RemoteFileChunk;
import run.ikaros.api.core.file.RemoteFileHandler;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.exception.NotFoundException;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.api.infra.utils.SystemVarUtils;
import run.ikaros.api.store.entity.FileEntity;
import run.ikaros.api.store.enums.FileType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.file.event.FileAddEvent;
import run.ikaros.server.core.file.event.FileRemoveEvent;
import run.ikaros.server.infra.utils.AesEncryptUtils;
import run.ikaros.server.plugin.ExtensionComponentsFinder;
import run.ikaros.server.store.entity.FileRemoteEntity;
import run.ikaros.server.store.repository.EpisodeFileRepository;
import run.ikaros.server.store.repository.FileRemoteRepository;
import run.ikaros.server.store.repository.FileRepository;

@Slf4j
@Service
public class FileServiceImpl implements FileService, ApplicationContextAware {
    private final FileRepository fileRepository;
    private final EpisodeFileRepository episodeFileRepository;
    private final FileRemoteRepository fileRemoteRepository;
    private final IkarosProperties ikarosProperties;
    private final R2dbcEntityTemplate template;
    private ApplicationContext applicationContext;
    private final ExtensionComponentsFinder extensionComponentsFinder;
    private final ReactiveCustomClient reactiveCustomClient;

    /**
     * Construct.
     */
    public FileServiceImpl(FileRepository fileRepository,
                           EpisodeFileRepository episodeFileRepository,
                           FileRemoteRepository fileRemoteRepository,
                           IkarosProperties ikarosProperties,
                           R2dbcEntityTemplate template,
                           ExtensionComponentsFinder extensionComponentsFinder,
                           ReactiveCustomClient reactiveCustomClient) {
        this.fileRepository = fileRepository;
        this.episodeFileRepository = episodeFileRepository;
        this.fileRemoteRepository = fileRemoteRepository;
        this.ikarosProperties = ikarosProperties;
        this.template = template;
        this.extensionComponentsFinder = extensionComponentsFinder;
        this.reactiveCustomClient = reactiveCustomClient;
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

            uploadName = uploadName.substring(0, uploadName.lastIndexOf("."));
            // save to database.
            String reactiveUrl = path2url(filePath, workDir.toString());
            var fileEntity = FileEntity.builder()
                .md5(FileUtils.checksum2Str(bytes, FileUtils.Hash.MD5))
                .url(reactiveUrl)
                .name(uploadName)
                .originalName(uploadName + "." + postfix)
                .size(uploadLength)
                .type(FileUtils.parseTypeByPostfix(postfix))
                .originalPath(filePath)
                .build();
            return save(fileEntity).then();
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
        return save(fileEntity);
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
                File file = new File(entity.getOriginalPath());
                if (file.exists()) {
                    file.delete();
                    log.debug("delete local file in path: {}", file.getAbsolutePath());
                }
                return entity;
            })
            .map(run.ikaros.api.core.file.File::new)
            .flatMap(file -> fileRepository.deleteById(id)
                .doOnSuccess(unused -> applicationContext.publishEvent(
                    new FileRemoveEvent(this, file.entity()))))
            .checkpoint("DeleteFileEntityByFileId")
            .then(episodeFileRepository.deleteAllByFileId(id))
            .doOnNext(lines -> {
                if (lines > 0) {
                    log.debug("Delete all episode file records, counts: [{}], "
                        + "when delete file entity for id [{}].", lines, id);
                }
            })
            .checkpoint("DeleteAllEpisodeFileByFileIdAfterDeleteFileEntity")
            .flatMapMany(unused -> fileRemoteRepository.findAllByFileId(id))
            .map(fileRemoteEntity -> {
                Optional<RemoteFileHandler> remoteFileHandlerOptional =
                    extensionComponentsFinder.getExtensions(RemoteFileHandler.class)
                        .stream().filter(remoteFileHandler ->
                            fileRemoteEntity.getRemote().equals(remoteFileHandler.remote()))
                        .findFirst();

                remoteFileHandlerOptional
                    .ifPresent(remoteFileHandler -> {
                        remoteFileHandler.delete(Path.of(fileRemoteEntity.getPath()));
                        log.debug("delete remote file: remote:[{}], path:[{}].",
                            fileRemoteEntity.getRemote(), fileRemoteEntity.getPath());
                    });
                return fileRemoteEntity;
            })
            .then();
    }

    @Override
    public Mono<FileEntity> save(FileEntity entity) {
        Assert.notNull(entity, "'entity' must not null.");
        return fileRepository.save(entity)
            .doOnSuccess(fileEntity ->
                applicationContext.publishEvent(new FileAddEvent(this, fileEntity)));
    }

    @Override
    public Mono<run.ikaros.api.core.file.File> upload(String fileName,
                                                      Flux<DataBuffer> dataBufferFlux) {
        Assert.notNull(dataBufferFlux, "'dataBufferFlux' must not null.");
        Assert.hasText(fileName, "'fileName' must has text.");

        return Mono.just(LocalDateTime.now())
            .map(importTime -> ikarosProperties.getWorkDir()
                .resolve(FileConst.IMPORT_DIR_NAME)
                .resolve(String.valueOf(importTime.getYear()))
                .resolve(String.valueOf(importTime.getMonthValue()))
                .resolve(String.valueOf(importTime.getDayOfMonth()))
                .resolve(String.valueOf(importTime.getHour()))
                .resolve(UUID.randomUUID().toString().replace("-", "")
                    + "-" + fileName))
            .flatMap(importPath -> writeToImportPath(dataBufferFlux, importPath))
            .map(path -> FileEntity.builder()
                .folderId(FileConst.DEFAULT_FOLDER_ID)
                .type(FileUtils.parseTypeByPostfix(FileUtils.parseFilePostfix(fileName)))
                .url(path2url(path.toString(), ikarosProperties.getWorkDir().toString()))
                .name(fileName)
                .originalPath(path.toString())
                .originalName(fileName)
                .md5(FileUtils.calculateFileHash(dataBufferFlux))
                .canRead(true)
                .createTime(LocalDateTime.now())
                .build())
            .flatMap(fileEntity -> FileUtils.calculateFileSize(dataBufferFlux)
                .map(fileEntity::setSize))
            .flatMap(fileRepository::save)
            .map(run.ikaros.api.core.file.File::new);
    }


    private Mono<FileEntity> doPushRemote(FileEntity fileEntity, String remote) {
        Optional<RemoteFileHandler> remoteFileHandlerOptional =
            extensionComponentsFinder.getExtensions(RemoteFileHandler.class)
                .stream()
                .filter(remoteFileHandler -> remote.equals(remoteFileHandler.remote()))
                .findFirst();
        if (remoteFileHandlerOptional.isEmpty()) {
            throw new RuntimeException("no remote file handler for remote: " + remote);
        }
        final RemoteFileHandler remoteFileHandler = remoteFileHandlerOptional.get();

        if (!remoteFileHandler.ready()) {
            throw new RuntimeException("please config remote plugin for remote:" + remote);
        }

        File file = new File(fileEntity.getOriginalPath());
        Path importPath = file.toPath();
        Path splitChunksPath = ikarosProperties.getWorkDir()
            .resolve("caches")
            .resolve("file")
            .resolve("split")
            .resolve(UUID.randomUUID().toString().replace("-", ""));
        FileUtils.mkdirsIfNotExists(splitChunksPath);
        Path encryptChunksPath = ikarosProperties.getWorkDir()
            .resolve("caches")
            .resolve("file")
            .resolve("encrypt")
            .resolve(UUID.randomUUID().toString().replace("-", ""));
        FileUtils.mkdirsIfNotExists(encryptChunksPath);

        // 默认分割单片 30MB
        List<Path> pathList = FileUtils.split(importPath, splitChunksPath, 1024 * 30);
        byte[] keyByteArray = AesEncryptUtils.generateKeyByteArray();


        // 加密
        for (Path path : pathList) {
            File file1 = path.toFile();
            String file1Name = file1.getName();
            try {
                byte[] bytes = AesEncryptUtils.encryptFile(keyByteArray, file1);
                Path targetFilePath = encryptChunksPath.resolve(file1Name);
                Files.write(targetFilePath, bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // 上传
        List<RemoteFileChunk> remoteFileIdList =
            remoteFileHandler.push(encryptChunksPath);

        // 清理本地文件
        try {
            // 分片文件
            FileUtils.deleteDirByRecursion(splitChunksPath.toString());
            // 分片加密文件
            FileUtils.deleteDirByRecursion(encryptChunksPath.toString());
            // 源文件
            importPath.toFile().delete();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 保存云端分片信息
        return Flux.fromStream(remoteFileIdList.stream())
            .flatMap(remoteFileChunk -> fileRemoteRepository.save(FileRemoteEntity.builder()
                .fileId(fileEntity.getId())
                .fileName(remoteFileChunk.getFileName())
                .remoteId(remoteFileChunk.getFileId())
                .remote(remote)
                .size(remoteFileChunk.getSize())
                .path(remoteFileChunk.getPath())
                .md5(remoteFileChunk.getMd5())
                .build()))
            // 更新文件状态
            .then(fileRepository.findById(fileEntity.getId()))
            .checkpoint("UpdateFileEntity")
            .map(fe -> fe.setAesKey(new String(keyByteArray, StandardCharsets.UTF_8)))
            .map(fileEntity1 -> fileEntity1.setUrl(""))
            .map(fileEntity1 -> fileEntity1.setCanRead(false))
            .flatMap(fileRepository::save);
    }

    private Mono<FileEntity> pushRemote(FileEntity fileEntity, String remote) {
        Assert.notNull(fileEntity, "'fileEntity' must not null.");
        Assert.hasText(remote, "'remote' must has text.");

        if (!fileEntity.getCanRead() && !StringUtils.hasText(fileEntity.getUrl())) {
            log.warn("skip push operate, current file entity exists in remote for file: {}",
                fileEntity);
            return Mono.just(fileEntity);
        }

        // 远端表存在对应的记录，代表已经 push 过
        return fileRemoteRepository.findAllByFileIdAndRemote(fileEntity.getId(), remote)
            .collectList()
            .filter(list -> Objects.isNull(list) || list.isEmpty())
            .flatMap(list -> doPushRemote(fileEntity, remote))
            .switchIfEmpty(Mono.just(fileEntity)
                .doOnSuccess(fe -> log.debug("current remote record exists, skip doPushRemote.")));
    }


    @Override
    public Mono<FileEntity> pushRemote(Long fileId, String remote) {
        Assert.isTrue(fileId > 0, "'fileId' must gt 0.");
        Assert.hasText(remote, "'remote' must has text.");
        return fileRepository.findById(fileId)
            .switchIfEmpty(
                Mono.error(new NotFoundException("not found file entity for id is " + fileId)))
            .flatMap(fileEntity -> pushRemote(fileEntity, remote));
    }

    private Mono<FileEntity> pullRemote(FileEntity fileEntity, String remote) {
        Assert.notNull(fileEntity, "'fileEntity' must not null.");
        Assert.hasText(remote, "'remote' must has text.");

        if (fileEntity.getCanRead() && StringUtils.hasText(fileEntity.getUrl())) {
            throw new RuntimeException("please push remote before pull.");
        }

        Optional<RemoteFileHandler> remoteFileHandlerOptional =
            extensionComponentsFinder.getExtensions(RemoteFileHandler.class)
                .stream()
                .filter(remoteFileHandler -> remote.equals(remoteFileHandler.remote()))
                .findFirst();
        if (remoteFileHandlerOptional.isEmpty()) {
            throw new RuntimeException("no remote file handler for remote: " + remote);
        }
        final RemoteFileHandler remoteFileHandler = remoteFileHandlerOptional.get();

        if (!remoteFileHandler.ready()) {
            throw new RuntimeException("please config remote plugin for remote:" + remote);
        }

        Path encryptChunkFilesPath = ikarosProperties.getWorkDir()
            .resolve("caches")
            .resolve("file")
            .resolve("encrypt")
            .resolve(UUID.randomUUID().toString().replace("-", ""));
        FileUtils.mkdirsIfNotExists(encryptChunkFilesPath);


        String aesKey = fileEntity.getAesKey();
        byte[] aesKeyBytes = aesKey.getBytes(StandardCharsets.UTF_8);
        Path decryptChunkFilesPath = ikarosProperties.getWorkDir()
            .resolve("caches")
            .resolve("file")
            .resolve("decrypt")
            .resolve(UUID.randomUUID().toString().replace("-", ""));
        FileUtils.mkdirsIfNotExists(decryptChunkFilesPath);


        LocalDateTime now = LocalDateTime.now();
        Path importFilePath = ikarosProperties.getWorkDir()
            .resolve(FileConst.IMPORT_DIR_NAME)
            .resolve(String.valueOf(now.getYear()))
            .resolve(String.valueOf(now.getMonthValue()))
            .resolve(String.valueOf(now.getDayOfMonth()))
            .resolve(String.valueOf(now.getHour()))
            .resolve(UUID.randomUUID().toString().replace("-", "")
                + "-" + fileEntity.getName());

        // 查询当前文件所有的远端ID
        return fileRemoteRepository.findAllByFileId(fileEntity.getId())
            .filter(fileRemoteEntity -> remote.equals(fileRemoteEntity.getRemote()))
            .map(FileRemoteEntity::getRemoteId)
            .collectList()
            // 拉取远端文件
            .map(list -> {
                remoteFileHandler.pull(encryptChunkFilesPath, list);
                return list;
            })
            .<List<String>>handle((list, sink) -> {
                File[] encryptChunkFiles = encryptChunkFilesPath.toFile().listFiles();
                if (encryptChunkFiles == null) {
                    sink.error(new RuntimeException(
                        "encrypt file dir is null for path: " + encryptChunkFilesPath));
                    return;
                }
                int index = 0;
                final int total = encryptChunkFiles.length;
                for (File encryptChunkFile : encryptChunkFiles) {
                    String name = encryptChunkFile.getName();
                    if (name.indexOf('-') > 0) {
                        name = name.substring(name.lastIndexOf("-") + 1);
                    }
                    Path decryptChunkFilePath = decryptChunkFilesPath.resolve(name);
                    try {
                        byte[] bytes = AesEncryptUtils.decryptFile(aesKeyBytes, encryptChunkFile);
                        Files.write(decryptChunkFilePath, bytes);
                        index++;
                        log.debug("current encrypt chunk file: {}/{}.", index, total);
                    } catch (IOException e) {
                        sink.error(new RuntimeException(e));
                        return;
                    }
                }
                sink.next(list);
            })
            // 合并文件片
            .map(list -> {
                List<Path> chunkFilePaths =
                    Arrays.stream(
                            Objects.requireNonNull(decryptChunkFilesPath.toFile().listFiles()))
                        .map(File::toPath)
                        .toList();
                FileUtils.synthesize(chunkFilePaths, importFilePath);
                return list;
            })
            // 清理临时文件
            .<List<String>>handle((list, sink) -> {
                try {
                    FileUtils.deleteDirByRecursion(decryptChunkFilesPath.toString());
                    FileUtils.deleteDirByRecursion(encryptChunkFilesPath.toString());
                } catch (IOException e) {
                    sink.error(new RuntimeException(e));
                    return;
                }
                sink.next(list);
            })
            // 更新文件URL
            .then(fileRepository.save(fileEntity.setCanRead(true).setUrl(
                path2url(importFilePath.toString(), ikarosProperties.getWorkDir().toString()))))
            ;
    }

    @Override
    public Mono<FileEntity> pullRemote(Long fileId, String remote) {
        Assert.isTrue(fileId > 0, "'fileId' must gt 0.");
        Assert.hasText(remote, "'remote' must has text.");
        return fileRepository.findById(fileId)
            .flatMap(fileEntity -> pullRemote(fileEntity, remote));
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

        log.debug("Merging all chunk files success, absolute path: {}", absolutePath);
        return absolutePath;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
