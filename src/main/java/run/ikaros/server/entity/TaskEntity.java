package run.ikaros.server.entity;

import run.ikaros.server.enums.TaskType;

import javax.persistence.Entity;


@Entity(name = "task")
public class TaskEntity extends BaseEntity {

    private String name;

    private TaskType type = TaskType.INTERNAL;

    private String internalTaskName;

    public String getName() {
        return name;
    }

    public TaskEntity setName(String name) {
        this.name = name;
        return this;
    }

    public TaskType getType() {
        return type;
    }

    public TaskEntity setType(TaskType type) {
        this.type = type;
        return this;
    }

    public String getInternalTaskName() {
        return internalTaskName;
    }

    public TaskEntity setInternalTaskName(String internalTaskName) {
        this.internalTaskName = internalTaskName;
        return this;
    }
}
