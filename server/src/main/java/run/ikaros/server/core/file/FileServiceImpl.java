package run.ikaros.server.core.file;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.entity.FileEntity;
import run.ikaros.api.store.enums.FilePlace;
import run.ikaros.api.store.enums.FileType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.file.event.FileAddEvent;
import run.ikaros.server.core.file.event.FileRemoveEvent;
import run.ikaros.server.infra.properties.IkarosProperties;
import run.ikaros.server.infra.utils.FileUtils;
import run.ikaros.server.infra.utils.SystemVarUtils;
import run.ikaros.server.store.repository.FileRepository;

@Slf4j
@Service
public class FileServiceImpl implements FileService, ApplicationContextAware {
    private final FileRepository fileRepository;
    private final IkarosProperties ikarosProperties;
    private final R2dbcEntityTemplate template;
    private ApplicationContext applicationContext;

    /**
     * Construct.
     */
    public FileServiceImpl(FileRepository fileRepository, IkarosProperties ikarosProperties,
                           R2dbcEntityTemplate template) {
        this.fileRepository = fileRepository;
        this.ikarosProperties = ikarosProperties;
        this.template = template;
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
                .place(FilePlace.LOCAL)
                .url(reactiveUrl)
                .name(uploadName + "." + postfix)
                .size(uploadLength)
                .type(FileUtils.parseTypeByPostfix(postfix))
                .originalPath(filePath)
                .build();
            fileEntity.setUpdateTime(LocalDateTime.now());
            fileEntity.setCreateTime(LocalDateTime.now());
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
        final FilePlace place = condition.getPlace();
        final FileType type = condition.getType();

        PageRequest pageRequest = PageRequest.of(page - 1, size);

        Flux<FileEntity> fileEntityFlux;
        Mono<Long> countMono;

        // todo 使用 R2dbcEntityTemplate 进行动态拼接
        if (place == null) {
            if (type == null) {
                fileEntityFlux = fileRepository.findAllByNameLike(fileNameLike, pageRequest);
                countMono = fileRepository.countAllByNameLike(fileNameLike);
            } else {
                fileEntityFlux =
                    fileRepository.findAllByNameLikeAndType(fileNameLike, type, pageRequest);
                countMono = fileRepository.countAllByNameLikeAndType(fileNameLike, type);
            }
        } else {
            if (type == null) {
                fileEntityFlux =
                    fileRepository.findAllByNameLikeAndPlace(fileNameLike, place, pageRequest);
                countMono = fileRepository.countAllByNameLikeAndPlace(fileNameLike, place);
            } else {
                fileEntityFlux =
                    fileRepository.findAllByNameLikeAndPlaceAndType(fileNameLike, place, type,
                        pageRequest);
                countMono =
                    fileRepository.countAllByNameLikeAndPlaceAndType(fileNameLike, place, type);
            }
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
        return findById(id)
            .flatMap(fileEntity -> fileRepository.deleteById(id)
                .doOnSuccess(unused ->
                    applicationContext.publishEvent(new FileRemoveEvent(this, fileEntity))))
            .then();
    }

    @Override
    public Mono<FileEntity> save(FileEntity entity) {
        Assert.notNull(entity, "'entity' must not null.");
        return fileRepository.save(entity)
            .doOnSuccess(fileEntity ->
                applicationContext.publishEvent(new FileAddEvent(this, fileEntity)));
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
