package run.ikaros.server.core.file.task;

import static run.ikaros.api.constant.AppConst.BLOCK_TIMEOUT;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import run.ikaros.api.core.file.RemoteFileChunk;
import run.ikaros.api.core.file.RemoteFileHandler;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.server.core.task.Task;
import run.ikaros.server.infra.utils.AesEncryptUtils;
import run.ikaros.server.plugin.ExtensionComponentsFinder;
import run.ikaros.server.store.entity.FileEntity;
import run.ikaros.server.store.entity.FileRemoteEntity;
import run.ikaros.server.store.entity.TaskEntity;
import run.ikaros.server.store.repository.FileRemoteRepository;
import run.ikaros.server.store.repository.FileRepository;
import run.ikaros.server.store.repository.TaskRepository;

@Slf4j
public class FilePush2RemoteTask extends Task {
    private final ApplicationContext applicationContext;
    private final Long fileId;
    private final String remote;

    /**
     * Construct.
     */
    public FilePush2RemoteTask(TaskEntity entity,
                               TaskRepository repository,
                               ApplicationContext applicationContext,
                               Long fileId, String remote) {
        super(entity, repository);
        this.applicationContext = applicationContext;
        this.fileId = fileId;
        this.remote = remote;
    }

    @Override
    protected String getTaskEntityName() {
        return this.getClass().getSimpleName() + "-" + fileId;
    }

    @Override
    protected void doRun() throws Exception {
        Assert.notNull(applicationContext, "'applicationContext' must not null.");
        Assert.notNull(getRepository(), "'repository' must not null.");
        Assert.isTrue(fileId > 0, "'fileId' must gt 0.");
        Assert.hasText(remote, "'remote' must has text.");

        // 获取一些需要的Bean
        final ExtensionComponentsFinder extensionComponentsFinder =
            applicationContext.getBean(ExtensionComponentsFinder.class);
        final FileRepository fileRepository = applicationContext.getBean(FileRepository.class);
        final IkarosProperties ikarosProperties =
            applicationContext.getBean(IkarosProperties.class);
        final FileRemoteRepository fileRemoteRepository =
            applicationContext.getBean(FileRemoteRepository.class);

        // 获取文件记录
        Optional<FileEntity> fileEntityOp = fileRepository.findById(fileId)
            .blockOptional(BLOCK_TIMEOUT);
        Assert.isTrue(fileEntityOp.isPresent(), "'fileEntity' must is present for id: " + fileId);
        FileEntity fileEntity = fileEntityOp.get();

        // 查询是否已经存在远端，已经推送过的不需要重复推送
        Optional<List<FileRemoteEntity>> fileRemoteEntitiesOp =
            fileRemoteRepository.findAllByFileId(fileEntity.getId())
                .collectList().blockOptional(BLOCK_TIMEOUT);
        if (fileRemoteEntitiesOp.isPresent() && fileRemoteEntitiesOp.get().size() > 0) {
            // 已经推送，更新文件记录即可
            Path localFilePath = Path.of(fileEntity.getFsPath());
            Files.deleteIfExists(localFilePath);
            log.debug("delete local file in path: {}", localFilePath);
            fileRepository.findById(fileEntity.getId())
                .checkpoint("UpdateFileEntity")
                .map(fileEntity1 -> fileEntity1.setUrl("").setCanRead(false).setFsPath(""))
                .flatMap(fileRepository::save).block(BLOCK_TIMEOUT);
            return;
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

        Path localFilePath = Path.of(fileEntity.getFsPath());
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

        // 默认分割单片 100MB
        List<Path> pathList = FileUtils.split(localFilePath, splitChunksPath, 1024 * 100);
        byte[] keyByteArray = AesEncryptUtils.generateKeyByteArray();

        // 更新任务总数
        getRepository().save(getEntity().setTotal((long) pathList.size())).block(BLOCK_TIMEOUT);

        // 加密
        log.info("starting encrypt all chunk files...");
        for (int i = 0; i < pathList.size(); i++) {
            java.io.File file1 = pathList.get(i).toFile();
            try {
                FileInputStream data = new FileInputStream(file1);
                Path targetFilePath = encryptChunksPath.resolve(file1.getName());
                java.io.File file2 = targetFilePath.toFile();
                FileOutputStream out = new FileOutputStream(file2);
                AesEncryptUtils.encryptInputStream(data, true, out, keyByteArray);
                log.info("current encrypt chunk file index : {}/{}", i + 1, pathList.size());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        log.info("end encrypt all chunk files...");

        // 上传
        List<Path> encryptFilePathList = Arrays.stream(
                Objects.requireNonNull(encryptChunksPath.toFile().listFiles()))
            .map(file1 -> Path.of(file1.toURI()))
            .toList();
        if (encryptFilePathList.isEmpty()) {
            throw new RuntimeException(
                "encrypt files must not empty in path: " + encryptChunksPath);
        }
        List<RemoteFileChunk> remoteFileChunkList = new ArrayList<>(encryptFilePathList.size());
        for (int i = 0; i < encryptFilePathList.size(); i++) {
            remoteFileChunkList.add(remoteFileHandler.push(encryptFilePathList.get(i)));
            // 更新任务进度
            getRepository().save(getEntity().setIndex((long) (i + 1))).block(BLOCK_TIMEOUT);
        }

        // 保存云端分片信息
        Flux.fromStream(remoteFileChunkList.stream())
            .flatMap(remoteFileChunk -> fileRemoteRepository.save(FileRemoteEntity.builder()
                .fileId(fileEntity.getId())
                .fileName(remoteFileChunk.getFileName())
                .remoteId(remoteFileChunk.getFileId())
                .remote(remote)
                .size(remoteFileChunk.getSize())
                .path(remoteFileChunk.getPath())
                .md5(remoteFileChunk.getMd5())
                .build())).blockLast(BLOCK_TIMEOUT);


        // 清理本地文件
        try {
            // 分片文件
            FileUtils.deleteDirByRecursion(splitChunksPath.toString());
            // 分片加密文件
            FileUtils.deleteDirByRecursion(encryptChunksPath.toString());
            // 源文件
            localFilePath.toFile().delete();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 更新文件状态
        fileRepository.findById(fileEntity.getId())
            .checkpoint("UpdateFileEntity")
            .map(fe -> fe.setAesKey(new String(keyByteArray, StandardCharsets.UTF_8)))
            .map(fileEntity1 -> fileEntity1.setUrl("").setCanRead(false).setFsPath(""))
            .flatMap(fileRepository::save).block(BLOCK_TIMEOUT);
    }
}
