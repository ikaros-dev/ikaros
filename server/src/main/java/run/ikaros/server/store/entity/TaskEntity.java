package run.ikaros.server.store.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.server.store.enums.TaskStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "task")
@Accessors(chain = true)
public class TaskEntity {
    @Id
    private Long id;
    private String name;
    private TaskStatus status;
    @Column("create_time")
    private LocalDateTime createTime;
    @Column("start_time")
    private LocalDateTime startTime;
    @Column("end_time")
    private LocalDateTime endTime;
    private Long total;
    private Long index;
    @Column("fail_message")
    private String failMessage;
}
