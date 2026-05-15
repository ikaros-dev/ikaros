package run.ikaros.api.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.util.UUID;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
public class Role implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private UUID id;

    private LocalDateTime createTime;

    private UUID createUid;

    private Boolean deleteStatus = false;

    private LocalDateTime updateTime;

    private UUID updateUid;

    private Long olVersion;

    private UUID parentId;

    private String name;

    private String description;
}
