package run.ikaros.server.core.file.task;

import static run.ikaros.api.infra.utils.FileUtils.path2url;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import run.ikaros.api.core.file.RemoteFileHandler;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.api.store.entity.FileEntity;
import run.ikaros.server.core.task.Task;
import run.ikaros.server.infra.utils.AesEncryptUtils;
import run.ikaros.server.plugin.ExtensionComponentsFinder;
import run.ikaros.server.store.entity.FileRemoteEntity;
import run.ikaros.server.store.entity.TaskEntity;
import run.ikaros.server.store.repository.FileRemoteRepository;
import run.ikaros.server.store.repository.FileRepository;
import run.ikaros.server.store.repository.TaskRepository;

@Slf4j
public class FilePull4RemoteTask extends Task {
    private final ApplicationContext applicationContext;
    private final Long fileId;
    private final String remote;

    /**
     * Construct.
     */
    public FilePull4RemoteTask(TaskEntity entity,
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
        final FileEntity fileEntity = fileEntityOp.get();

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

        Path filePath = Path.of(FileUtils.buildAppUploadFilePath(
            ikarosProperties.getWorkDir().toString(),
            FileUtils.parseFilePostfix(fileEntity.getOriginalName())
        ));

        // 查询当前文件所有的远端ID
        Optional<List<String>> remoteFileIdListOp =
            fileRemoteRepository.findAllByFileId(fileEntity.getId())
                .filter(fileRemoteEntity -> remote.equals(fileRemoteEntity.getRemote()))
                .map(FileRemoteEntity::getRemoteId)
                .collectList().blockOptional();
        if (remoteFileIdListOp.isEmpty()) {
            throw new RuntimeException(
                "not remote record for file: " + fileEntity.getOriginalName());
        }
        List<String> remoteFileIdList = remoteFileIdListOp.get();

        // 更新总数
        getRepository().save(getEntity().setTotal((long) remoteFileIdList.size())).block();

        for (int i = 0; i < remoteFileIdList.size(); i++) {
            remoteFileHandler.pull(encryptChunkFilesPath, remoteFileIdList.get(i));
            // 更新进度
            getRepository().save(getEntity().setIndex((long) (i + 1))).block();
        }

        // 解密文件分片
        File[] encryptChunkFiles = encryptChunkFilesPath.toFile().listFiles();
        if (encryptChunkFiles == null) {
            throw new RuntimeException(
                "encrypt file dir is null for path: " + encryptChunkFilesPath);
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
                throw new RuntimeException(e);
            }
        }

        // 合并文件分片
        List<Path> chunkFilePaths =
            Arrays.stream(
                    Objects.requireNonNull(decryptChunkFilesPath.toFile().listFiles()))
                .map(File::toPath)
                .toList();
        FileUtils.synthesize(chunkFilePaths, filePath);

        // 清理临时文件
        try {
            FileUtils.deleteDirByRecursion(decryptChunkFilesPath.toString());
            FileUtils.deleteDirByRecursion(encryptChunkFilesPath.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 更新文件状态 是否可读 和 URL
        fileRepository.save(fileEntity.setCanRead(true)
            .setOriginalPath(filePath.toString())
            .setUrl(path2url(filePath.toString(),
                ikarosProperties.getWorkDir().toString()))).block();

    }
}
