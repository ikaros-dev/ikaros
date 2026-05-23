package run.ikaros.server.store.repository;

import java.util.UUID;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.DirectoryBindingWorkflowEntity;

public interface DirectoryBindingWorkflowRepository
    extends BaseRepository<DirectoryBindingWorkflowEntity> {

    Mono<DirectoryBindingWorkflowEntity> findByTaskId(UUID taskId);

    Mono<DirectoryBindingWorkflowEntity> findByDirectoryId(UUID directoryId);
}
