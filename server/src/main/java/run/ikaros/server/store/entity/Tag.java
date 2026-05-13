package run.ikaros.server.store.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

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
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;

    private Object id;

    private String type;

    private Object masterId;

    private String name;

    private Object userId;

    private LocalDateTime createTime;

    private String color;
}
