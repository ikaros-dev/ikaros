package run.ikaros.server.core.file.task;

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
import run.ikaros.api.core.file.File;
import run.ikaros.api.core.file.Folder;
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
public class FolderPush2RemoteTask extends Task {
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
    public FolderPush2RemoteTask(TaskEntity entity,
                                 TaskRepository repository,
                                 Folder folder, String remote,
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

        // 获取所有未推送至远端的文件
        List<File> files = new ArrayList<>();
        updateCanReadFiles(folder, files);

        // 更新任务总数
        getRepository().save(getEntity().setTotal((long) files.size())).block();

        // 推送所有待推送的文件
        for (int i = 0; i < files.size(); i++) {
            pushFile2Remote(files.get(i), remote);
            // 更新任务进度
            getRepository().save(getEntity().setIndex((long) (i + 1))).block();
        }

    }

    private void pushFile2Remote(File file, String remote) throws IOException {
        // 获取文件记录
        Optional<FileEntity> fileEntityOp = fileRepository.findById(file.getId()).blockOptional();
        if (fileEntityOp.isEmpty()) {
            throw new IllegalArgumentException(
                "'fileEntity' must is present for id: " + file.getId());
        }
        final FileEntity fileEntity = fileEntityOp.get();

        // 查询是否已经存在远端，已经推送过的不需要重复推送
        Optional<List<FileRemoteEntity>> fileRemoteEntitiesOp =
            fileRemoteRepository.findAllByFileId(fileEntity.getId())
                .collectList().blockOptional();
        if (fileRemoteEntitiesOp.isPresent() && fileRemoteEntitiesOp.get().size() > 0) {
            // 已经推送，更新文件记录即可
            Path localFilePath =
                Path.of(FileUtils.url2path(fileEntity.getUrl(), ikarosProperties.getWorkDir()));
            Files.deleteIfExists(localFilePath);
            log.debug("delete local file in path: {}", localFilePath);
            fileRepository.findById(fileEntity.getId())
                .checkpoint("UpdateFileEntity")
                .map(fileEntity1 -> fileEntity1.setUrl("").setCanRead(false).setOriginalPath(""))
                .flatMap(fileRepository::save).block();
            return;
        }

        Path localFilePath =
            Path.of(FileUtils.url2path(fileEntity.getUrl(), ikarosProperties.getWorkDir()));
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

        // 加密
        for (Path path : pathList) {
            java.io.File file1 = path.toFile();
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

        try {
            List<RemoteFileChunk> remoteFileChunkList = new ArrayList<>(encryptFilePathList.size());
            for (Path path : encryptFilePathList) {
                remoteFileChunkList.add(remoteFileHandler.push(path));
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


            // 更新文件状态
            fileRepository.findById(fileEntity.getId())
                .checkpoint("UpdateFileEntity")
                .map(fe -> fe.setAesKey(new String(keyByteArray, StandardCharsets.UTF_8)))
                .map(fileEntity1 -> fileEntity1.setUrl("").setCanRead(false).setOriginalPath(""))
                .flatMap(fileRepository::save).block();
        } finally {
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
        }
    }

    /**
     * Get all can read files from children.
     */
    private List<File> updateCanReadFiles(Folder folder, List<File> files) {
        if (folder.hasFolder()) {
            for (Folder f : folder.getFolders()) {
                files.addAll(updateCanReadFiles(f, files));
            }
        }
        if (folder.hasFile()) {
            files.addAll(folder.getFiles()
                .stream()
                .filter(File::getCanRead)
                .toList());
        }
        return files;
    }
}
