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
public class Attachment implements Serializable {

    private static final long serialVersionUID = 1L;

    private Object id;

    private Object parentId;

    private String type;

    private String url;

    private String path;

    private String fsPath;

    private String name;

    private Long size;

    private LocalDateTime updateTime;

    private Boolean deleted;

    private Object driverId;

    private String sha1;
}
