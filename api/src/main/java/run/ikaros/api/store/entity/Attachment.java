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
public class Attachment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long parentId;

    private String type;

    private String url;

    private String path;

    private String fsPath;

    private String name;

    private Long size;

    private LocalDateTime updateTime;

    private Boolean deleted;

    private Long driverId;

    private String sha1;
}
