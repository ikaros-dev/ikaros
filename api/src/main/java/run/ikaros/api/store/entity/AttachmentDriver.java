package run.ikaros.api.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
@TableName("attachment_driver")
public class AttachmentDriver implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Boolean enable;

    private String driverType;

    private String driverName;

    private String mountName;

    private String remotePath;

    private Long driverOrder;

    private String driverComment;

    private String refreshToken;

    private String accessToken;

    private LocalDateTime expireTime;

    private Long listPageSize;

    private String rootDirId;

    private Long requestLimit;

    private String userId;

    private String userName;

    private String avatar;

    private Long spaceTotal;

    private Long spaceUse;
}
