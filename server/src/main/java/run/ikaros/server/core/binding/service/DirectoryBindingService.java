package run.ikaros.server.core.binding.service;

import java.util.UUID;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.store.entity.DirectoryBindingWorkflowEntity;

public interface DirectoryBindingService {

    /**
     * Bind a single directory to a subject.
     *
     * @param keyword optional keyword to search subject, overrides cleanName if set
     */
    Mono<DirectoryBindingWorkflowEntity> bindDirectory(UUID directoryId,
                                                       SubjectSyncPlatform platform,
                                                       String keyword);

    /**
     * Bind a single directory to a subject without keyword.
     */
    default Mono<DirectoryBindingWorkflowEntity> bindDirectory(UUID directoryId,
                                                                SubjectSyncPlatform platform) {
        return bindDirectory(directoryId, platform, null);
    }

    /**
     * Batch bind all subdirectories under a parent directory.
     */
    Mono<Void> bindDirectories(UUID parentDirectoryId, SubjectSyncPlatform platform);

    /**
     * Find workflow by workflow ID.
     */
    Mono<DirectoryBindingWorkflowEntity> findWorkflowById(UUID workflowId);

    /**
     * Find workflow by task ID.
     */
    Mono<DirectoryBindingWorkflowEntity> findWorkflowByTaskId(UUID taskId);
}
