package run.ikaros.api.store.entity;

import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.util.UUID;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Attachment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private UUID id;

    private UUID parentId;

    private String type;

    private String url;

    private String path;

    private String fsPath;

    private String name;

    private Long size;

    private LocalDateTime updateTime;

    private Boolean deleted;

    private UUID driverId;

    private String sha1;
}
