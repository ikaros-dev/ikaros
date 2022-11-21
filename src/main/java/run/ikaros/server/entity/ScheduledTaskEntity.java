package run.ikaros.server.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.time.LocalDateTime;

@Entity(name = "scheduled_task")
public class ScheduledTaskEntity extends TaskEntity {

    private String cron;

    @Column(name = "datd_time")
    private LocalDateTime deadTime;

    @Column(name = "start_time")
    private LocalDateTime startTime;


    public String getCron() {
        return cron;
    }

    public ScheduledTaskEntity setCron(String cron) {
        this.cron = cron;
        return this;
    }

    public LocalDateTime getDeadTime() {
        return deadTime;
    }

    public ScheduledTaskEntity setDeadTime(LocalDateTime deadTime) {
        this.deadTime = deadTime;
        return this;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public ScheduledTaskEntity setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
        return this;
    }

}
