package run.ikaros.server.store.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * <p>
 * 
 * </p>
 *
 * @author chivehao
 * @since 2026-05-13
 */
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
