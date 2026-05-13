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
public class Person implements Serializable {

    private static final long serialVersionUID = 1L;

    private Object id;

    private LocalDateTime createTime;

    private Object createUid;

    private Boolean deleteStatus;

    private LocalDateTime updateTime;

    private Object updateUid;

    private Long olVersion;

    private String name;

    private String infobox;

    private String summary;
}
