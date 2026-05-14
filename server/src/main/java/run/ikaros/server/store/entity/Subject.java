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
public class Subject implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;

    private LocalDateTime createTime;

    private UUID createUid;

    private Boolean deleteStatus;

    private LocalDateTime updateTime;

    private UUID updateUid;

    private Long olVersion;

    private String type;

    private String name;

    private String nameCn;

    private String cover;

    private String infobox;

    private String summary;

    private Boolean nsfw;

    private LocalDateTime airTime;

    private Double score;
}
