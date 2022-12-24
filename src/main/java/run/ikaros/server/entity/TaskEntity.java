package run.ikaros.server.entity;

import jakarta.persistence.Column;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.ikaros.server.enums.TaskType;

import jakarta.persistence.Entity;

import java.time.LocalDateTime;

@Data
@Entity(name = "task")
@EqualsAndHashCode(callSuper = true)
public class TaskEntity extends BaseEntity {

    private String name;

    private TaskType type = TaskType.INTERNAL;

    private String internalTaskName;
    private String cron;

    @Column(name = "datd_time")
    private LocalDateTime deadTime;

    @Column(name = "start_time")
    private LocalDateTime startTime;

}
