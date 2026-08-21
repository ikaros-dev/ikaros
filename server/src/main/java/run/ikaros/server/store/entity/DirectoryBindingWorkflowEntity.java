package run.ikaros.server.store.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.api.store.enums.TaskStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table(name = "directory_binding_workflow")
public class DirectoryBindingWorkflowEntity {
    @Id
    private UUID id;
    @Column("task_id")
    private UUID taskId;
    @Column("directory_id")
    private UUID directoryId;
    @Column("directory_name")
    private String directoryName;
    @Column("subject_id")
    private UUID subjectId;
    private SubjectSyncPlatform platform;
    @Column("platform_id")
    private String platformId;
    private TaskStatus status;
    @Column("current_step")
    private String currentStep;
    @Column("step_statuses")
    private String stepStatuses;
    @Column("create_time")
    private LocalDateTime createTime;
    @Column("end_time")
    private LocalDateTime endTime;
    @Column("fail_message")
    private String failMessage;
}
