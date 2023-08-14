package run.ikaros.server.core.file.task;

import static run.ikaros.api.constant.AppConst.BLOCK_TIMEOUT;
import static run.ikaros.api.infra.utils.FileUtils.path2url;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import run.ikaros.api.core.file.File;
import run.ikaros.api.core.file.Folder;
import run.ikaros.api.core.file.RemoteFileHandler;
import run.ikaros.api.infra.exception.NotFoundException;
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
public class FolderPull4RemoteTask extends Task {
    private final Folder folder;
    private final String remote;
    private final ApplicationContext applicationContext;
    private FileRepository fileRepository;
    private IkarosProperties ikarosProperties;
    private FileRemoteRepository fileRemoteRepository;
    private RemoteFileHandler remoteFileHandler;


    /**
     * Construct.
     */
    public FolderPull4RemoteTask(TaskEntity entity,
                                 TaskRepository repository, Folder folder, String remote,
                                 ApplicationContext applicationContext) {
        super(entity, repository);
        this.folder = folder;
        this.remote = remote;
        this.applicationContext = applicationContext;
    }

    @Override
    protected String getTaskEntityName() {
        return this.getClass().getSimpleName() + "-" + folder.getId();
    }

    @Override
    protected void doRun() throws Exception {
        Assert.notNull(applicationContext, "'applicationContext' must not null.");
        Assert.notNull(getRepository(), "'repository' must not null.");
        Assert.notNull(folder, "'folder' must not null.");
        Assert.hasText(remote, "'remote' must has text.");
        // 获取一些需要的Bean
        final ExtensionComponentsFinder extensionComponentsFinder =
            applicationContext.getBean(ExtensionComponentsFinder.class);
        fileRepository = applicationContext.getBean(FileRepository.class);
        ikarosProperties =
            applicationContext.getBean(IkarosProperties.class);
        fileRemoteRepository =
            applicationContext.getBean(FileRemoteRepository.class);

        // 获取远端处理器
        Optional<RemoteFileHandler> remoteFileHandlerOptional =
            extensionComponentsFinder.getExtensions(RemoteFileHandler.class)
                .stream()
                .filter(remoteFileHandler -> remote.equals(remoteFileHandler.remote()))
                .findFirst();
        if (remoteFileHandlerOptional.isEmpty()) {
            throw new RuntimeException("no remote file handler for remote: " + remote);
        }
        remoteFileHandler = remoteFileHandlerOptional.get();

        if (!remoteFileHandler.ready()) {
            throw new RuntimeException("please config remote plugin for remote:" + remote);
        }

        // 获取所有推送至远端的文件
        List<File> files = new ArrayList<>();
        updateCanNotReadFiles(folder, files);

        // 更新任务总数
        getRepository().save(getEntity().setTotal((long) files.size())).block(BLOCK_TIMEOUT);

        // 拉取所有待拉取的文件
        for (int i = 0; i < files.size(); i++) {
            pullFile2Remote(files.get(i), remote);
            // 更新任务进度
            getRepository().save(getEntity().setIndex((long) (i + 1))).block(BLOCK_TIMEOUT);
        }
    }

    private void pullFile2Remote(File file, String remote) {
        // 获取文件实体记录
        final FileEntity fileEntity = fileRepository.findById(file.getId()).block(BLOCK_TIMEOUT);
        if (fileEntity == null) {
            throw new NotFoundException("not found file entity for id=" + file.getId());
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

        final Path filePath = Path.of(FileUtils.buildAppUploadFilePath(
            ikarosProperties.getWorkDir().toString(),
            FileUtils.parseFilePostfix(file.getName())
        ));

        // 查询当前文件所有的远端ID
        Optional<List<String>> remoteFileIdListOp =
            fileRemoteRepository.findAllByFileId(fileEntity.getId())
                .filter(fileRemoteEntity -> remote.equals(fileRemoteEntity.getRemote()))
                .map(FileRemoteEntity::getRemoteId)
                .collectList().blockOptional(BLOCK_TIMEOUT);
        if (remoteFileIdListOp.isEmpty()) {
            throw new RuntimeException(
                "not remote record for file: " + fileEntity.getName());
        }
        List<String> remoteFileIdList = remoteFileIdListOp.get();

        for (String s : remoteFileIdList) {
            remoteFileHandler.pull(encryptChunkFilesPath, s);
        }

        // 解密文件分片
        java.io.File[] encryptChunkFiles = encryptChunkFilesPath.toFile().listFiles();
        if (encryptChunkFiles == null) {
            throw new RuntimeException(
                "decrypt file dir is null for path: " + encryptChunkFilesPath);
        }
        int index = 0;
        final int total = encryptChunkFiles.length;
        log.info("starting decrypt chunk file ...");
        for (java.io.File encryptChunkFile : encryptChunkFiles) {
            String name = encryptChunkFile.getName();
            if (name.indexOf('-') > 0) {
                name = name.substring(name.lastIndexOf("-") + 1);
            }
            Path decryptChunkFilePath = decryptChunkFilesPath.resolve(name);
            try {
                FileInputStream data = new FileInputStream(encryptChunkFile);
                FileOutputStream out = new FileOutputStream(decryptChunkFilePath.toFile());
                AesEncryptUtils.decryptInputStream(data, true, out, aesKeyBytes);
                index++;
                log.info("current decrypt chunk file: {}/{}.", index, total);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        log.info("end decrypt chunk file ...");

        // 合并文件分片
        List<Path> chunkFilePaths =
            Arrays.stream(
                    Objects.requireNonNull(decryptChunkFilesPath.toFile().listFiles()))
                .map(java.io.File::toPath)
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
            .setFsPath(filePath.toString())
            .setUrl(path2url(filePath.toString(),
                ikarosProperties.getWorkDir().toString()))).block(BLOCK_TIMEOUT);
    }

    private List<File> updateCanNotReadFiles(Folder folder, List<File> files) {
        if (folder.hasFolder()) {
            for (Folder f : folder.getFolders()) {
                files.addAll(updateCanNotReadFiles(f, files));
            }
        }
        if (folder.hasFile()) {
            files.addAll(folder.getFiles()
                .stream()
                .filter(file -> !file.getCanRead())
                .toList());
        }
        return files;
    }
}
