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
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Object id;

    private LocalDateTime createTime;

    private Object createUid;

    private Boolean deleteStatus;

    private LocalDateTime updateTime;

    private Object updateUid;

    private Long olVersion;

    private String avatar;

    private String email;

    private Boolean enable;

    private String introduce;

    private String nickname;

    private Boolean nonLocked;

    private String password;

    private String site;

    private String telephone;

    private String username;
}
