package run.ikaros.server.core.file.task;

import java.io.File;
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
        getEntity().setName(this.getClass().getSimpleName() + "-" + fileId);
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
        Optional<FileEntity> fileEntityOp = fileRepository.findById(fileId).blockOptional();
        Assert.isTrue(fileEntityOp.isPresent(), "'fileEntity' must is present for id: " + fileId);
        FileEntity fileEntity = fileEntityOp.get();

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

        // 更新任务总数
        getRepository().save(getEntity().setTotal((long) pathList.size())).block();

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
            getRepository().save(getEntity().setIndex((long) (i + 1))).block();
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
                .build())).blockLast();


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


        // 更新文件状态
        fileRepository.findById(fileEntity.getId())
            .checkpoint("UpdateFileEntity")
            .map(fe -> fe.setAesKey(new String(keyByteArray, StandardCharsets.UTF_8)))
            .map(fileEntity1 -> fileEntity1.setUrl("").setCanRead(false).setOriginalPath(""))
            .flatMap(fileRepository::save).block();
    }
}
