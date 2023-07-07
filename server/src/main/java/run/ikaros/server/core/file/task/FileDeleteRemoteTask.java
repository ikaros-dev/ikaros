package run.ikaros.server.core.file.task;

import static run.ikaros.api.constant.AppConst.BLOCK_TIMEOUT;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import run.ikaros.api.constant.AppConst;
import run.ikaros.api.core.file.RemoteFileHandler;
import run.ikaros.server.core.task.Task;
import run.ikaros.server.plugin.ExtensionComponentsFinder;
import run.ikaros.server.store.entity.FileRemoteEntity;
import run.ikaros.server.store.entity.TaskEntity;
import run.ikaros.server.store.repository.FileRemoteRepository;
import run.ikaros.server.store.repository.TaskRepository;

@Slf4j
public class FileDeleteRemoteTask extends Task {
    private final ApplicationContext applicationContext;
    private final Long fileId;

    /**
     * Construct.
     */
    public FileDeleteRemoteTask(TaskEntity entity,
                                TaskRepository repository,
                                ApplicationContext applicationContext,
                                Long fileId) {
        super(entity, repository);
        this.applicationContext = applicationContext;
        this.fileId = fileId;
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

        // 获取一些需要的Bean
        final ExtensionComponentsFinder extensionComponentsFinder =
            applicationContext.getBean(ExtensionComponentsFinder.class);
        final FileRemoteRepository fileRemoteRepository =
            applicationContext.getBean(FileRemoteRepository.class);

        List<RemoteFileHandler> extensions =
            extensionComponentsFinder.getExtensions(RemoteFileHandler.class);

        List<FileRemoteEntity> fileRemoteEntities = fileRemoteRepository.findAllByFileId(fileId)
            .collectList().block(AppConst.BLOCK_TIMEOUT);
        if (fileRemoteEntities == null) {
            log.warn("not found file remote records for file id: {}", fileId);
            return;
        }

        // 更新总数
        getRepository().save(getEntity().setTotal((long) fileRemoteEntities.size()))
            .block(BLOCK_TIMEOUT);

        for (int i = 0; i < fileRemoteEntities.size(); i++) {
            FileRemoteEntity fileRemoteEntity = fileRemoteEntities.get(i);
            String remote = fileRemoteEntity.getRemote();
            Optional<RemoteFileHandler> remoteFileHandlerOptional = extensions.stream()
                .filter(remoteFileHandler -> remote.equals(remoteFileHandler.remote()))
                .findFirst();
            remoteFileHandlerOptional.ifPresent(remoteFileHandler -> {
                remoteFileHandler.delete(fileRemoteEntity.getPath());
                log.debug("delete remote file: remote:[{}], path:[{}].",
                    fileRemoteEntity.getRemote(), fileRemoteEntity.getPath());
            });
            // 更新进度
            getRepository().save(getEntity().setIndex((long) (i + 1))).block(BLOCK_TIMEOUT);
        }

        fileRemoteRepository.deleteAllByFileId(fileId)
            .doOnSuccess(unused -> log.debug("delete all remote records for file id: " + fileId))
            .block(BLOCK_TIMEOUT);
    }
}
