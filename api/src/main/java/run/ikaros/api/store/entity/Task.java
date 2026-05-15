package run.ikaros.api.store.entity;

import java.io.Serializable;
import java.util.UUID;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
public class Task implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;

    private String name;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long total;

    private Long index;

    private String failMessage;
}
