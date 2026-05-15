package run.ikaros.api.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.UUID;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@TableName("ikuser")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private UUID id;

    private LocalDateTime createTime;

    private Object createUid;

    private Boolean deleteStatus = false;

    private LocalDateTime updateTime;

    private Object updateUid;

    private Long olVersion;

    private String avatar;

    private String email;

    private Boolean enable = true;

    private String introduce;

    private String nickname;

    private Boolean nonLocked = true;

    private String password;

    private String site;

    private String telephone;

    private String username;
}
