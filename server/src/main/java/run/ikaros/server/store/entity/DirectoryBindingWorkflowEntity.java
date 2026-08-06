package run.ikaros.server.store.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.api.store.enums.TaskStatus;

/** 记录目录绑定任务的目标、执行状态和本地扫描快照. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table(name = "directory_binding_workflow")
public class DirectoryBindingWorkflowEntity {
    /** 工作流标识. */
    @Id
    private UUID id;
    /** 乐观锁版本. */
    @Version
    private Long version;
    /** 当前提交任务的标识. */
    @Column("task_id")
    private UUID taskId;
    /** 被绑定目录的附件标识. */
    @Column("directory_id")
    private UUID directoryId;
    /** 被绑定目录的显示名称. */
    @Column("directory_name")
    private String directoryName;
    /** 目标条目标识. */
    @Column("subject_id")
    private UUID subjectId;
    /** 远程绑定平台，本地绑定时为空. */
    private SubjectSyncPlatform platform;
    /** 本地目录绑定的扫描模式. */
    @Column("local_mode")
    private String localMode;
    /** 本地扫描确认后的状态快照. */
    @Column("local_scan_state")
    private String localScanState;
    /** 远程平台中的条目标识. */
    @Column("platform_id")
    private String platformId;
    /** 工作流执行状态. */
    private TaskStatus status;
    /** 当前执行步骤说明. */
    @Column("current_step")
    private String currentStep;
    /** 各步骤执行状态的序列化结果. */
    @Column("step_statuses")
    private String stepStatuses;
    /** 工作流创建时间. */
    @Column("create_time")
    private LocalDateTime createTime;
    /** 工作流结束时间. */
    @Column("end_time")
    private LocalDateTime endTime;
    /** 工作流失败原因. */
    @Column("fail_message")
    private String failMessage;
}
