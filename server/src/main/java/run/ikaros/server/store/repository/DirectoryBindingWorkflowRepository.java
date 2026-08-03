package run.ikaros.server.store.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.DirectoryBindingWorkflowEntity;

/** 提供目录绑定工作流的响应式持久化操作。 */
public interface DirectoryBindingWorkflowRepository
    extends BaseRepository<DirectoryBindingWorkflowEntity> {

    Mono<DirectoryBindingWorkflowEntity> findByTaskId(UUID taskId);

    Mono<DirectoryBindingWorkflowEntity> findByDirectoryId(UUID directoryId);

    /**
     * 查询指定本地目录、条目和扫描模式对应的工作流。
     *
     * @param directoryId 目录附件标识
     * @param subjectId 条目标识
     * @param localMode 本地扫描模式
     * @return 本地工作流，不存在时为空
     */
    @Query("""
        SELECT * FROM directory_binding_workflow
        WHERE directory_id = :directoryId
          AND subject_id = :subjectId
          AND local_mode = :localMode
          AND platform IS NULL
        LIMIT 1
        """)
    Mono<DirectoryBindingWorkflowEntity> findLocalWorkflow(UUID directoryId, UUID subjectId,
                                                            String localMode);
}
