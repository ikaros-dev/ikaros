package run.ikaros.api.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Character implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private LocalDateTime createTime;

    private Long createUid;

    private Boolean deleteStatus;

    private LocalDateTime updateTime;

    private Long updateUid;

    private Long olVersion;

    private String name;

    private String infobox;

    private String summary;
}
