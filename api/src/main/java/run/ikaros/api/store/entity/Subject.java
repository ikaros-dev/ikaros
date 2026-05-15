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
public class Subject implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
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
